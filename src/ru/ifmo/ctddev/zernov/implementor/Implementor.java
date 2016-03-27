package ru.ifmo.ctddev.zernov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Creator of compiling inheritor (or jar file including compiled .class file of this inheritor) and
 * save it to <tt>path</tt> with <tt>Impl</tt> suffix
 * @see info.kgeorgiy.java.advanced.implementor.Impler
 * @see info.kgeorgiy.java.advanced.implementor.JarImpler
 * @see info.kgeorgiy.java.advanced.implementor.ImplerException
 */
public class Implementor implements Impler, JarImpler {
    private Class<?> token;
    private BufferedWriter out;
    private Path path;

    /**
     * Create compiling inheritor of class or interface provided by<tt>token</tt> and
     * save java class to <tt>root</tt> with <tt>Impl</tt> suffix.
     * If file or folder can't create prints {@link java.io.IOException} message
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException If inheritor can't be create
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        path = root.resolve((token.getPackage() == null ? "" :
                token.getPackage().getName().replace('.', File.separatorChar))
                + File.separatorChar + token.getSimpleName() + "Impl.java");
        this.token = token;
        if (!token.isInterface()) {
            inheritanceCheck(token);
        }
        try {
            try {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            } catch (Exception e) {
//                   Occurred when file is already exists
//                e.printStackTrace();
            }
            out = Files.newBufferedWriter(path,Charset.defaultCharset());
            write(token.getPackage() == null ? "" : "package " + token.getPackage().getName() + ";\n");
            write(Modifier.toString(token.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.INTERFACE) + " class " +
                    token.getSimpleName() + "Impl " + (token.isInterface() ? "implements " : "extends ") + token.getCanonicalName());
            write("{\n");
            HashMap<String, Method> tokenMethods = new HashMap<>();
            putMethods(tokenMethods, token.getMethods());
            putMethods(tokenMethods, token.getDeclaredMethods());
            Class<?> sup = token.getSuperclass();
            while (sup != null) {
                putMethods(tokenMethods, sup.getDeclaredMethods());
                sup = sup.getSuperclass();
            }
//        get super abstract methods

            Constructor[] tokenConstructors = token.getDeclaredConstructors();
            boolean hasNotDefaultConstructions = tokenConstructors.length != 0;
            for (Constructor constructor : tokenConstructors) {
                if (constructor.getParameterCount() == 0 && !Modifier.isPrivate(constructor.getModifiers())) {
                    hasNotDefaultConstructions = false;
                }
            }
            if (hasNotDefaultConstructions && !token.isInterface()) {
                int i = 0;
                for (; i < tokenConstructors.length; i++) {
                    Constructor constructor = tokenConstructors[i];
                    if (createExecutable(constructor, false)) {
                        break;
                    }
                }
                if (i == tokenConstructors.length) {
                    throw new ImplerException("No non-private constructors in super class");
                }
            }

            for (String key : tokenMethods.keySet()) {
                Method method = tokenMethods.get(key);
                if (Modifier.isFinal(method.getModifiers()) || Modifier.isNative(method.getModifiers()) || !
                        Modifier.isAbstract(method.getModifiers())) {
                    continue;
                }
                createExecutable(method, true);
                if (method.getReturnType().isPrimitive()) {
                    switch (method.getReturnType().getName()) {
                        case "void":
                            break;
                        case "boolean":
                            write("\t\treturn false;\n");
                            break;
                        default:
                            write("\t\treturn 0;\n");
                    }
                } else {
                    write("\t\treturn null;\n");
                }
                write("\t}\n\n");
            }
            write("}");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create .jar file which contains java file created by {@link #implement}
     * save jar file to <tt>jarFile</tt> contains java file with <tt>Impl</tt> suffix.
     * If file or folder can't create prints {@link java.io.IOException} message
     *
     * @param token type token to create implementation for.
     * @param jarFile jar file directory.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException If inheritor can't be create
     * @see info.kgeorgiy.java.advanced.implementor.JarImpler
     * @see info.kgeorgiy.java.advanced.implementor.ImplerException
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        implement(token,Paths.get(System.getProperty("user.home") + File.separatorChar + "temp"));
        try {
            JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
            javac.run(null,null,null,path.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            OutputStream subStream = Files.newOutputStream(jarFile);
            JarOutputStream jarOutputStream = new JarOutputStream(subStream);
            FileInputStream in = new FileInputStream(path.toString().substring(0,path.toString().length()-4) + "class");
            ZipEntry entry = new ZipEntry((token.getPackage() == null ? "" :
                    token.getPackage().getName().replace('.', '/'))
                    + '/' + token.getSimpleName() + "Impl.class");
            jarOutputStream.putNextEntry(entry);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                jarOutputStream.write(buffer, 0, len);
            }
            in.close();
            jarOutputStream.closeEntry();
            jarOutputStream.close();
            subStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sugar method to write string in output stream in simpler way
     * @param outString string that should be written into stream
     * @throws IOException when string can't be written into {@link #out} stream
     */
    private void write(String outString) throws IOException {
        out.write(outString);
    }

    /**
     * Write {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor} into output java file
     *
     *
     * @param executable {@link java.lang.reflect.Method} or {@link java.lang.reflect.Constructor} of parent
     *                                                   that should be inherited
     * @param isMethod must be true if and only if <tt>executable</tt> is {@link java.lang.reflect.Method}
     *                 otherwise output class may not compile
     * @return false, if <tt>executable</tt> is private and can't be inherited
     * @throws IOException if error occured while writing to file
     */
    private boolean createExecutable(Executable executable, boolean isMethod) throws IOException {
        Method method = null;
        if (isMethod) {
            method = (Method) executable;
        }
        if (Modifier.isPrivate(executable.getModifiers())) {
            return false;
        }
        write("\t" + Modifier.toString((executable.getModifiers() & ~Modifier.ABSTRACT) & ~Modifier.TRANSIENT) + " " +
                (isMethod ? method.getReturnType().getCanonicalName() + " " + executable.getName() + " "
                        : " " + token.getSimpleName() + "Impl ") + "(");
        Class[] parameterTypes = executable.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            write((Modifier.isTransient(executable.getModifiers()) && i == parameterTypes.length - 1 ?
                    parameterTypes[i].getCanonicalName().substring(0, parameterTypes[i].getCanonicalName().length() - 2) +
                    "..." : parameterTypes[i].getCanonicalName()) + " var" + i + (i == parameterTypes.length - 1 ? "" : ", "));
        }
        write(")");
        Class[] exceptionThrows = executable.getExceptionTypes();
        write(exceptionThrows.length == 0 ? "{\n" : " throws ");
        for (int i = 0; i < exceptionThrows.length; i++) {
            write(exceptionThrows[i].getCanonicalName() + (i == exceptionThrows.length - 1 ? "{\n" : ", "));
        }
        if (!isMethod) {
            write("\t\tsuper(");
            for (int i = 0; i < parameterTypes.length; i++) {
                write("var" + i + (i == parameterTypes.length - 1 ? ");\n\t}\n" : ", "));
            }
        }
        return true;
    }

    /**
     * Add methods from <tt>methodsArray</tt> array to <tt>tokenMethods</tt> HashMap
     * @param tokenMethods HashMap that stores methods
     * @param methodsArray array of methods that should be added to map
     */
    private void putMethods(HashMap<String, Method> tokenMethods, Method[] methodsArray) {
        for (Method method : methodsArray) {
            String hash = method.getName() + Arrays.hashCode(method.getParameterTypes());
            while (tokenMethods.containsKey(hash) && !(tokenMethods.get(hash).getName().equals(method.getName())
                    && Arrays.equals(tokenMethods.get(hash).getParameterTypes(),(method.getParameterTypes())))){
                hash = hash + new Random().nextInt();
            }
            tokenMethods.put(hash, method);
        }
    }

    /**
     * Check if class could be inherited or not
     * @param token type token to create implementation for.
     * @throws ImplerException in case of this type token can't be inherited
     */
    private void inheritanceCheck(Class<?> token) throws ImplerException {
        if (token.isPrimitive()) {
            throw new ImplerException("Can't extend primitive");
        }
        if (token.isArray()) {
            throw new ImplerException("Can't extend array");
        }
        int modifier = token.getModifiers();
        if (Modifier.isFinal(modifier)) {
            throw new ImplerException("Can't extend final class");
        }
        if (token.isMemberClass()) {
            throw new ImplerException("Can't extend member class out of the scope");
        }
        if (token.isAnonymousClass()) {
            throw new ImplerException("Can't extend anonymous class");
        }
        if (token.equals(java.lang.Enum.class)) {
            throw new ImplerException("Can't extend enum directly");
        }
        if (token.isEnum()) {
            throw new ImplerException("Can't extend enum");
        }
    }

    /**
     * Invokes main methods {@link #implement(Class, Path)} if second <tt>argument</tt> is path
     * and {@link #implementJar(Class, Path)} if it's path to jar file
     *
     * @param args first param should be <tt>token</tt> and the second is <tt>path</tt>
     *
     * @see #implement(Class, Path)
     * @see #implementJar(Class, Path)
     **/
    public static void main(String[] args) {
        Implementor implementor = new Implementor();
        try {
            String inputPath = args[1];
            if (inputPath.indexOf(".jar", inputPath.length() - 4) == inputPath.length() - 4) {
                implementor.implementJar(Class.forName(args[0]), Paths.get(inputPath));
            } else {
                implementor.implement(Class.forName(args[0]), Paths.get(inputPath));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
