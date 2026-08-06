package com.dj.insulink.shared.feature.reminders.data.remote

import com.dj.insulink.shared.core.time.currentLocalTimeOfDay
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseReminderRemoteDataSource(
    private val firestore: FirebaseFirestore
) : ReminderRemoteDataSource {

    override suspend fun pushReminder(userId: String, reminder: Reminder) {
        firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .update(DOCUMENT_FIELD_REMINDERS, FieldValue.arrayUnion(reminder))
            .await()
    }

    override suspend fun deleteReminder(userId: String, reminder: Reminder) {
        try {
            val userDocumentRef = firestore.collection(COLLECTION_NAME_USERS).document(userId)

            val snapshot = userDocumentRef.get().await()
            val reminders =
                snapshot.get(DOCUMENT_FIELD_REMINDERS) as? List<Map<String, Any>> ?: emptyList()

            val updatedReminders = reminders.filter { reminderMap ->
                val idMatches = (reminderMap["id"] as? Number)?.toLong() == reminder.id

                !idMatches
            }

            if (reminders.size != updatedReminders.size) {
                userDocumentRef.update(DOCUMENT_FIELD_REMINDERS, updatedReminders).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun fetchAllReminders(userId: String): List<Reminder> {
        val document = firestore.collection(COLLECTION_NAME_USERS)
            .document(userId)
            .get()
            .await()

        val remindersData =
            document.get(DOCUMENT_FIELD_REMINDERS) as? List<Map<String, Any>> ?: emptyList()

        return remindersData.map { reminderMap ->
            val reminderTime = (reminderMap["time"] as? Number)?.toLong() ?: 0

            Reminder(
                id = (reminderMap["id"] as? Number)?.toLong() ?: 0,
                userId = reminderMap["userId"] as? String ?: "",
                title = reminderMap["title"] as? String ?: "",
                reminderType = ReminderType.fromName(reminderMap["reminderType"] as? String)
                    ?: ReminderType.MEAL_REMINDER,
                isDoneForToday = localTimeOfDay(reminderTime) < currentLocalTimeOfDay(),
                time = reminderTime,
            )
        }
    }
}

private const val COLLECTION_NAME_USERS = "users"
private const val DOCUMENT_FIELD_REMINDERS = "reminders"
