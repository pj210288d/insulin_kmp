package com.dj.insulink.shared.feature.reminders.data.remote

import com.dj.insulink.shared.core.auth.IosAuthTokenProvider
import com.dj.insulink.shared.core.firestore.FirestoreRestClient
import com.dj.insulink.shared.core.firestore.FirestoreValue
import com.dj.insulink.shared.core.time.currentLocalTimeOfDay
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import kotlinx.serialization.json.JsonElement

private const val USERS_COLLECTION = "users"
private const val FIELD_REMINDERS = "reminders"

// Faza 2 (cloud sync) - iOS ekvivalent Android-ovog FirebaseReminderRemoteDataSource.kt.
// isDoneForToday se NE čuva u Firestore-u - računa se pri čitanju (isti core/time helper kao
// Android, već platform-agnostičan) iz "time" polja.
class FirestoreRestReminderRemoteDataSource(
    private val firestoreClient: FirestoreRestClient,
    private val tokenProvider: IosAuthTokenProvider
) : ReminderRemoteDataSource {

    override suspend fun pushReminder(userId: String, reminder: Reminder) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_REMINDERS, idToken)
        val updated = current + reminder.toFirestoreValue().toJson()
        firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_REMINDERS, updated, idToken)
    }

    override suspend fun deleteReminder(userId: String, reminder: Reminder) {
        val idToken = tokenProvider.currentIdToken()
        val current = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_REMINDERS, idToken)
        val updated = current.filterNot { FirestoreValue.longOf(it, "id") == reminder.id }
        if (updated.size != current.size) {
            firestoreClient.setArrayField(USERS_COLLECTION, userId, FIELD_REMINDERS, updated, idToken)
        }
    }

    override suspend fun fetchAllReminders(userId: String): List<Reminder> {
        val idToken = tokenProvider.currentIdToken()
        val elements = firestoreClient.getArrayField(USERS_COLLECTION, userId, FIELD_REMINDERS, idToken)
        return elements.mapNotNull { it.toReminderOrNull() }
    }
}

private fun Reminder.toFirestoreValue() = FirestoreValue.MapVal(
    mapOf(
        "id" to FirestoreValue.IntNum(id),
        "userId" to FirestoreValue.Str(userId),
        "title" to FirestoreValue.Str(title),
        "reminderType" to FirestoreValue.Str(reminderType.name),
        "time" to FirestoreValue.IntNum(time)
    )
)

private fun JsonElement.toReminderOrNull(): Reminder? {
    val id = FirestoreValue.longOf(this, "id") ?: return null
    val reminderTime = FirestoreValue.longOf(this, "time") ?: 0L
    return Reminder(
        id = id,
        userId = FirestoreValue.stringOf(this, "userId").orEmpty(),
        title = FirestoreValue.stringOf(this, "title").orEmpty(),
        reminderType = ReminderType.fromName(FirestoreValue.stringOf(this, "reminderType"))
            ?: ReminderType.MEAL_REMINDER,
        isDoneForToday = localTimeOfDay(reminderTime) < currentLocalTimeOfDay(),
        time = reminderTime
    )
}
