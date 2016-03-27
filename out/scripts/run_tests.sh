#/bin/bash

jar=$1
libs=/home/gleb/IdeaProjects/java-advanced-2016/lib/*:/home/gleb/IdeaProjects/java-advanced-2016/artifacts/$1Test.jar
out_folder=/home/gleb/IdeaProjects/JavaAdvanced/out/production/JavaAdvanced/
class=$2
package=$2
declare -l package
package=$package
tester=info.kgeorgiy.java.advanced.$package.Tester


java -cp $libs:$out_folder $tester $3 ru.ifmo.ctddev.zernov.$package.$class "${4}"
