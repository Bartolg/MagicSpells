-keep class org.tensorflow.** { *; }
-keep class com.example.fluidsim.ml.** { *; }

-keepclassmembers class * extends org.tensorflow.lite.Interpreter$Options$Builder { *; }

-dontwarn org.tensorflow.**
