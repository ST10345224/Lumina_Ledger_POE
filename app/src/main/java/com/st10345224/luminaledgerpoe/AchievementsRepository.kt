package com.st10345224.luminaledgerpoe

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose // Import for callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow // Import callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await


/**
 * Repository class to handle all data operations related to user achievements with Firestore.
 */
class AchievementsRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val TAG = "AchievementsRepo"

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not logged in.")
    }

    /**
     * Fetches a Flow of all UserAchievementProgress documents for the current user.
     * This flow will emit new lists whenever the underlying data in Firestore changes.
     */
    fun getAllUserAchievementProgress(): Flow<List<UserAchievementProgress>> = callbackFlow {
        val userId = getCurrentUserId()
        val achievementsCollectionRef = firestore
            .collection("Users")
            .document(userId)
            .collection("Achievements")

        // Register the snapshot listener
        val subscription = achievementsCollectionRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen failed for user achievements.", e)
                // Offer an empty list or an error state to the flow
                // Use trySend for non-suspending emit in callbackFlow
                trySend(emptyList()).isSuccess // Attempt to send empty list on error
                close(e) // Close the flow with the exception
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val progressList = snapshot.toObjects(UserAchievementProgress::class.java)
                trySend(progressList).isSuccess // Send the new list of progress
            } else {
                trySend(emptyList()).isSuccess // Send empty list if snapshot is null
            }
        }

        // The awaitClose block ensures the listener is removed when the flow is cancelled or completed
        awaitClose {
            Log.d(TAG, "Removing listener for all user achievement progress for user: $userId")
            subscription.remove()
        }
    }.catch { e ->
        Log.e(TAG, "Flow caught error for getAllUserAchievementProgress", e)
        emit(emptyList()) // Emit empty list on error
    }


    /**
     * Fetches a Flow of a single UserAchievementProgress document for a given achievement ID.
     * Emits `null` if the document doesn't exist.
     */
    fun getUserAchievementProgress(achievementId: String): Flow<UserAchievementProgress?> = callbackFlow {
        val userId = getCurrentUserId()
        val documentRef = firestore
            .collection("Users")
            .document(userId)
            .collection("Achievements")
            .document(achievementId)

        // Register the snapshot listener
        val subscription = documentRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen failed for single achievement progress: $achievementId", e)
                trySend(null).isSuccess // Attempt to send null on error
                close(e) // Close the flow with the exception
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(UserAchievementProgress::class.java)).isSuccess
            } else {
                trySend(null).isSuccess // Document does not exist or snapshot is null
            }
        }

        // Ensure the listener is removed when the flow is no longer collected
        awaitClose {
            Log.d(TAG, "Removing listener for single achievement progress: $achievementId")
            subscription.remove()
        }
    }.catch { e ->
        Log.e(TAG, "Flow caught error for getUserAchievementProgress $achievementId", e)
        emit(null) // Emit null on error
    }

    /**
     * Counts the number of expense documents for the current user.
     * It queries the "Expenses" collection and filters by the "userId" field.
     */
    suspend fun countUserExpenses(): Long {
        val userId = getCurrentUserId()
        return try {
            val snapshot =
                firestore.collection("Expenses") // Collection name: "Expenses" (case-sensitive)
                    .whereEqualTo(
                        "userID",
                        userId
                    )             // Field name: "userID" (case-sensitive)
                    .get()
                    .await()
            snapshot.size().toLong() // Returns the number of documents found
        } catch (e: Exception) {
            Log.e(TAG, "Error counting user expenses for user: $userId", e)
            0L // Return 0 on error
        }
    }


    /**
     * Updates or creates a specific achievement's progress document in Firestore.
     * This is a suspend function for one-time write operations.
     */
    suspend fun updateUserAchievementProgress(progress: UserAchievementProgress) {
        val userId = getCurrentUserId()
        try {
            firestore
                .collection("Users")
                .document(userId)
                .collection("Achievements")
                .document(progress.achievementId)
                .set(progress)
                .await()
            Log.d(TAG, "Achievement progress updated for ${progress.achievementId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating achievement progress for ${progress.achievementId}: ${e.message}", e)
        }
    }
}