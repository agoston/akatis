all: classes/Akatis.class

tmpclasses/Akatis.class: src/Akatis.java
	javac -d tmpclasses -bootclasspath /home/geza/mobile/WTK104/lib/midpapi.zip -classpath tmpclasses:classes src/Akatis.java

classes/Akatis.class: tmpclasses/Akatis.class
	/home/geza/mobile/WTK104/bin/preverify -classpath /home/geza/mobile/WTK104/lib/midpapi.zip tmpclasses -d classes
