# MongoDB driver proguard rules
-keep class com.mongodb.** { *; }
-keep class org.bson.** { *; }
-keep class javax.security.sasl.** { *; }
-dontwarn javax.naming.**
-dontwarn javax.management.**
-dontwarn javax.security.auth.callback.**
