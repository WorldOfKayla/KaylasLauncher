-injars 'C:/Users/Aiden/Documents/GitHub/FoxesLauncher/build/libs/FoxesLauncher-1.20.4-Ascension-all.jar'
-outjars 'C:\Users\Aiden\Documents\GitHub\FoxesLauncher\build\libs\FoxesLauncher-1.20.4-Ascension-obf.jar'

-dontoptimize
-dontshrink

-libraryjars 'C:\Users\Aiden\.jdks\liberica-full-17.0.13\jmods'


# Указание словарей для обфускации
#-packageobfuscationdictionary class_names.txt
#-classobfuscationdictionary fox_words.txt


# Исключение важных классов и пакетов от обфускации
-keep class !org.foxesworld.** { *; }

# Сохранение атрибутов
-keepattributes *Annotation*,EnclosingMethod,Signature,InnerClasses

# Исключения для аннотаций Log4j
-keepclassmembers class * {
    public static void main(java.lang.String[]);
    <init>(java.util.Map);
    <init>(java.util.Collection);
    *** entrySet();
}

-keep @org.apache.logging.log4j.core.config.plugins.* class * { *; }
-keep class org.apache.logging.log4j.core.config.plugins.util.** { *; }
-keep class org.apache.logging.log4j.core.config.** { *; }

# Исключения для различных классов в проекте
-keepclassmembers class * {
    <methods>;
}

-keep class org.foxesworld.engine.game.argsReader.libraries.** { *; }
-keepclassmembers class com.google.gson.** { *; }

# Подавление предупреждений
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn org.foxesworld.**
-dontwarn org.apache.logging.log4j.**
-dontwarn com.google.gson.**
-dontwarn java.sql.**
-dontwarn com.sun.jna.**
-dontwarn **.Unsafe
-dontwarn java.lang.management.**
-dontwarn sun.misc.**
-dontwarn org.xml.**
-dontwarn org.w3c.**
-dontwarn javax.annotation.**
-dontwarn javax.xml.**
-dontwarn javax.net.ssl.**

# Сохранение членов классов в некоторых библиотеках
-keepclassmembers class com.google.gson.internal.** {
    <init>(...);
    <fields>;
    <methods>;
}
-keepclassmembers class com.sun.jna.** {
    <init>(...);
    <fields>;
    <methods>;
}
-keepclassmembers class org.apache.logging.log4j.** {
    <init>(...);
    <fields>;
    <methods>;
}

# Сохранение атрибутов для рефлексии
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Подавление предупреждений для java.lang.invoke и java.util.concurrent
-dontwarn java.lang.invoke.*
-dontwarn java.util.concurrent.*

# Дополнительные правила для усиления обфускации
-adaptclassstrings
-overloadaggressively
-flattenpackagehierarchy ''
-repackageclasses ''
-allowaccessmodification
#-dontpreverify
