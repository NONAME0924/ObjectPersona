# ProGuard rules for ObjectPersona

# Keep Room entities
-keep class com.objectpersona.app.data.db.entity.** { *; }

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Persona model (used for JSON parsing in future phases)
-keep class com.objectpersona.app.data.model.** { *; }
