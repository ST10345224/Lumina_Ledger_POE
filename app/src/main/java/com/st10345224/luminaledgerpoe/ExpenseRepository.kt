// In ExpensesRepository.kt
package com.st10345224.luminaledgerpoe

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date

class ExpensesRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val TAG = "ExpensesRepository"

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not logged in.")
    }

    /**
     * Fetches a Flow of expense documents for the current user within a specified date range.
     * Expenses are ordered by the 'date' field.
     *
     * @param startDate The start Timestamp for the query (inclusive).
     * @param endDate The end Timestamp for the query (inclusive).
     * @return A Flow emitting a List of Expense objects.
     */
    fun getExpensesInDateRange(startDate: Timestamp, endDate: Timestamp): Flow<List<Expense>> = callbackFlow {
        val userId = getCurrentUserId()
        Log.d(TAG, "Setting up listener for expenses for user: $userId from ${startDate.toDate()} to ${endDate.toDate()}")

        val expensesCollectionRef = firestore
            .collection("Expenses")
            .whereEqualTo("userID", userId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.ASCENDING)

        val subscription = expensesCollectionRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen failed for expenses in date range for user: $userId", e)
                // If the channel is already closed, trySend will return false.
                // It's safer to close with the exception if trySend fails.
                close(e) // Closing the channel with the exception will propagate it to the collector
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val expenses = snapshot.documents.mapNotNull { document -> // Use mapNotNull for safer mapping
                    try {
                        val expense = Expense(
                            exID = document.id,
                            UserID = document.getString("userID") ?: "",
                            Category = document.getString("category") ?: "",
                            exAmount = document.getDouble("exAmount") ?: 0.0,
                            Date = document.getTimestamp("date") ?: Timestamp.now(),
                            exDescription = document.getString("exDescription") ?: "",
                            exPhotoString = document.getString("exPhotoString") ?: "",
                            Currency = document.getString("currency") ?: "",
                            exTitle = document.getString("exTitle") ?: ""
                        )
                        Log.d(TAG, "Mapped expense: ${expense.exTitle}, Amount: ${expense.exAmount}, Date: ${expense.Date.toDate()}")
                        expense
                    } catch (mapError: Exception) {
                        Log.e(TAG, "Error mapping document ${document.id} to Expense: ${mapError.message}", mapError)
                        null // Skip this document if mapping fails
                    }
                }
                Log.d(TAG, "Fetched ${expenses.size} expenses for user: $userId. Sending to flow...")
                if (trySend(expenses).isFailure) { // Check if sending was successful
                    Log.w(TAG, "Failed to send expenses to flow for user: $userId. Channel might be closed or full.")
                }
            } else {
                Log.d(TAG, "Snapshot was null for expenses in date range. Sending empty list.")
                if (trySend(emptyList()).isFailure) {
                    Log.w(TAG, "Failed to send empty list to flow for user: $userId.")
                }
            }
        }

        // AwaitClose ensures the listener is removed when the flow collector cancels or completes.
        awaitClose {
            Log.d(TAG, "Removing listener for expenses in date range for user: $userId")
            subscription.remove()
        }
    }.catch { e ->
        // This catch block will handle exceptions that occur within the callbackFlow's lambda
        // or during the collection of this flow
        Log.e(TAG, "Flow caught error for getExpensesInDateRange (outer catch)", e)
        emit(emptyList()) // Emit empty list on error
    }

    fun getAllExpensesForCurrentUser() = flow<List<Expense>> {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            emit(emptyList())
            return@flow
        }

        // Fetch all expenses from Firestore
        val snapshot = firestore.collection("Expenses")
            .whereEqualTo("userID", currentUserUid) // Filter by user ID on Firestore
            .get()
            .await()

        val expenses = snapshot.documents.mapNotNull { document ->
            try {
                // Field names match Firestore document structure exactly
                Expense(
                    exID = document.id, // Use document.id for the document ID
                    UserID = document.getString("userID") ?: "",
                    Category = document.getString("category") ?: "Uncategorized",
                    exAmount = document.getDouble("exAmount") ?: 0.0,
                    Date = document.getTimestamp("date") ?: Timestamp.now(), // Use getTimestamp
                    exDescription = document.getString("exDescription") ?: "",
                    exPhotoString = document.getString("exPhotoString") ?: "",
                    Currency = document.getString("currency") ?: "ZAR",
                    exTitle = document.getString("exTitle") ?: "Expense"
                )
            } catch (e: Exception) {
                Log.e("ExpensesRepository", "Error mapping expense document: ${e.message}", e)
                null
            }
        }
        emit(expenses)
    }.catch { e ->
        Log.e("ExpensesRepository", "Error fetching all user expenses: ${e.message}", e)
        throw e // Re-throw to be caught by the ViewModel
    }
}


