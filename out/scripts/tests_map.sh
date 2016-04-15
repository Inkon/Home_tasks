#/bin/bash

jar=ParallelMapper
libs=/home/gleb/IdeaProjects/java-advanced-2016/lib/*:/home/gleb/IdeaProjects/java-advanced-2016/artifacts/ParallelMapperTest.jar
out_folder=/home/gleb/IdeaProjects/JavaAdvanced/out/production/JavaAdvanced/
class=ParallelMapper
package=mapper
declare -l package
package=$package
tester=info.kgeorgiy.java.advanced.$package.Tester

java -cp $libs:$out_folder $tester $1 ru.ifmo.ctddev.zernov.$package.$class,ru.ifmo.ctddev.zernov.concurrent.Concurrent "${2}"
