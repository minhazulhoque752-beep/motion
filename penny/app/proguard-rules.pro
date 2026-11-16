# Penny – R8 rules.
# Room entities/DAOs are kept by Room's own consumer rules; Compose and AndroidX ship theirs too.
# Keep broadcast receivers referenced from the manifest (R8 keeps these automatically, listed for clarity).
-keep class app.penny.notifications.** extends android.content.BroadcastReceiver
# Keep line numbers for readable crash traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
