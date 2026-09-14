# MongoDB driver proguard rules
-keep class com.mongodb.** { *; }
-keep class org.bson.** { *; }
-keep class javax.security.sasl.** { *; }
-keep class javax.security.auth.callback.** { *; }
-dontwarn javax.naming.**
-dontwarn javax.management.**
