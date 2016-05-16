package ru.ifmo.ctddev.zernov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;
import javafx.util.Pair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * WebCrawler based on implementation of <tt>Crawler</tt> interface.
 * Provides download list of contains links of gained url via <tt>downloader</tt>, using
 * <tt>downloaders</tt> plus <tt>extractors</tt> threads to download. Downloading per host limited by
 * <tt>perHost</tt>
 *
 *  @see Crawler
 */
public class WebCrawler implements Crawler {
    private Downloader downloader;
    private int perHost;
    private final List<String> resultList = new ArrayList<>();
    private Map<String, IOException> resultMap;
    private ThreadPoolExecutor executorDownloader, executorExtractor;

    /**
     * Configures options of crawler -- setting amount of <tt>downloaders</tt>, <tt>extractors</tt> and
     * <tt>perHost</tt> limitation, which will be used by <tt>downloader</tt> in order to call
     * {@link #download(String, int)}
     *
     * @see Downloader
     * @see Document
     * @see URLUtils#getHost(String)
     * @param downloader downloader which used to dowbload documents
     * @param downloaders limit of simultaneously working with <tt>downloader</tt> threads
     * @param extractors limit of extractors -- workers with <tt>downloader's</tt> {@link Document document}
     * @param perHost limit of downloading from {@link URLUtils#getHost(String) host}
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        BlockingQueue<Runnable> downloaderTasks = new ArrayBlockingQueue<>(1000, true);
        BlockingQueue<Runnable> executorTasks = new ArrayBlockingQueue<>(1000);
        executorDownloader = new ThreadPoolExecutor(downloaders, downloaders, 0, TimeUnit.SECONDS, downloaderTasks);
        executorExtractor = new ThreadPoolExecutor(extractors, extractors, 0, TimeUnit.SECONDS, executorTasks);
        this.perHost = perHost;
    }

    private class DownloadCallable implements Callable<Pair<Document, String>> {
        private String url;

        private DownloadCallable(String url) {
            this.url = url;
        }

        @Override
        public Pair<Document, String> call() throws Exception {
            try {
                Document result = downloader.download(url);
                synchronized (resultList) {
                    resultList.add(url);
                }
                return new Pair<>(result, url);
            } catch (IOException e) {
                resultMap.put(url, e);
            }
            return null;
        }
    }

    private class ExtractorCallable implements Callable<List<String>> {
        private Document result;
        private String url;

        private ExtractorCallable(Pair<Document, String> p) {
            this.result = p.getKey();
            this.url = p.getValue();
        }

        @Override
        public List<String> call() {
            try {
                List<String> cur;
                cur = result.extractLinks();
                return cur;
            } catch (IOException e) {
                resultMap.put(url, e);
            }
            return null;
        }
    }

    /**
     * Walking in tree down from root set by <tt>url</tt> and building from all
     * urls contained in <tt>root</tt> limited to <tt>depths</tt>. Result is
     * list, containing all visited nodes of tree and map describes occurred {@link IOException exceptions}
     * If thread was interrupted while obtaining some url or error occurred while calculating,
     * then this node and all his unique children would not be included in list, and error would be printed
     *
     * @see IOException
     * @see InterruptedException
     * @see ExecutionException
     * @param url set root of three
     * @param depth set depth of visiting tree
     * @return list of all visited urls and map of occurred errors
     */
    @Override
    public Result download(String url, int depth) {
        HashMap<String, Integer> counter = new HashMap<>();
        HashSet<String> downloaded = new HashSet<>();
        resultList.clear();
        resultMap = new ConcurrentHashMap<>();
        downloaded.add(url);
        Collection<Future<Pair<Document, String>>> futures = new CopyOnWriteArrayList<>();
        Collection<Pair<Future<List<String>>, String>> futuresExec = new LinkedList<>();

        futures.add(executorDownloader.submit(new DownloadCallable(url)));

        for (int i = 2; i <= depth; i++) {
            for (Future<Pair<Document, String>> future : futures) {
                try {
                    Pair<Document, String> p = future.get();
                    if (p != null) {
                        resultList.add(p.getValue());
                        futuresExec.add(new Pair<>(executorExtractor.submit(new ExtractorCallable(p)),
                                p.getValue()));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            HashMap<String, ArrayDeque<String>> map = new HashMap<>();
            for (Pair<Future<List<String>>, String> pair : futuresExec) {
                Future<List<String>> future = pair.getKey();
                try {
                    List<String> list = future.get();
                    if (list != null) {

                        for (String elem : list) {
                            try {
                                String key = URLUtils.getHost(elem);
                                if (map.containsKey(key)) {
                                    map.get(key).add(elem);
                                } else {
                                    ArrayDeque<String> deque = new ArrayDeque<>();
                                    deque.add(elem);
                                    map.put(key, deque);
                                }
                            } catch (MalformedURLException e) {
                                if (map.containsKey("")) {
                                    map.get("").add(elem);
                                } else {
                                    ArrayDeque<String> deque = new ArrayDeque<>();
                                    deque.add(elem);
                                    map.put("", deque);
                                }
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            futures.clear();
            futuresExec.clear();
            ArrayDeque<Future<Pair<Document, String>>> subDeque = new ArrayDeque<>();
            ArrayDeque<Future<Pair<Document, String>>> restDeque = new ArrayDeque<>();
            for (String key : map.keySet()) {
                while (!map.get(key).isEmpty()) {
                    int count = counter.containsKey(key) ? counter.get(key) : 0;
                    if (count != perHost | key.equals("")) {
                        String curUrl = map.get(key).remove();
                        counter.put(key, count + 1);
                        if (!downloaded.contains(curUrl)) {
                            downloaded.add(curUrl);
                            if (!resultMap.containsKey(url)) {
                                subDeque.add(executorDownloader.submit(new DownloadCallable(curUrl)));
                            }
                        }
                    } else {
                        break;
                    }
                }
            }
            while (!subDeque.isEmpty() || !restDeque.isEmpty()) {
                while (!subDeque.isEmpty()) {
                    Future<Pair<Document, String>> future = subDeque.remove();
                    futures.add(future);
                    try {
                        boolean wasDownloaded = true;
                        while (wasDownloaded) {
                            wasDownloaded = false;
                            Pair<Document, String> p = future.get();
                            try {
                                if (p != null) {
                                    String key = URLUtils.getHost(p.getValue());
                                    if (map.containsKey(key) && !map.get(key).isEmpty()) {
                                        String curUrl = map.get(key).remove();
                                        if (!downloaded.contains(curUrl)) {
                                            downloaded.add(curUrl);
                                            restDeque.add(executorDownloader.submit(new DownloadCallable(curUrl)));
                                        } else {
                                            wasDownloaded = true;
                                        }
                                    }
                                }
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                subDeque.addAll(restDeque);
                restDeque.clear();
            }
        }

        for (Future<Pair<Document, String>> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return new Result(resultList, resultMap);
    }

    /**
     * Stop all working threads immediately. Result of calling {@link #download} is unpredictable
     */
    @Override
    public void close() {
        executorDownloader.shutdownNow();
        executorExtractor.shutdownNow();
    }
}