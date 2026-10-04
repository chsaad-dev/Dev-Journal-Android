package com.devjournal.data.repository

import android.net.Uri
import android.os.Build
import com.devjournal.BuildConfig
import com.devjournal.data.model.BugReport
import com.devjournal.data.remote.CloudinaryUploader
import com.devjournal.domain.repository.BugReportRepository
import com.devjournal.util.NetworkMonitor
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BugReportRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryUploader: CloudinaryUploader,
    private val networkMonitor: NetworkMonitor
) : BugReportRepository {

    override suspend fun submitBugReport(report: BugReport, screenshotUri: Uri?): Result<Unit> {
        if (!networkMonitor.isOnline()) {
            return Result.failure(IOException("You are currently offline. Connect to the internet to submit a report."))
        }

        return try {
            var uploadedUrl = report.screenshotUrl
            if (screenshotUri != null) {
                val uploadResult = cloudinaryUploader.uploadImage(screenshotUri)
                if (uploadResult.isFailure) {
                    return Result.failure(
                        uploadResult.exceptionOrNull()
                            ?: IOException("Failed to upload screenshot. Please try again.")
                    )
                }
                uploadedUrl = uploadResult.getOrDefault("")
            }

            val finalDeviceInfo = if (report.deviceInfo.isNotBlank()) {
                report.deviceInfo
            } else {
                "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}, App v${BuildConfig.VERSION_NAME}"
            }

            val finalAppVersion = if (report.appVersion.isNotBlank()) {
                report.appVersion
            } else {
                BuildConfig.VERSION_NAME
            }

            val bugReportRef = firestore.collection("bugReports").document()
            val data = hashMapOf<String, Any?>(
                "id" to bugReportRef.id,
                "userId" to report.userId,
                "userEmail" to report.userEmail,
                "title" to report.title.trim(),
                "description" to report.description.trim(),
                "screenshotUrl" to uploadedUrl,
                "deviceInfo" to finalDeviceInfo,
                "appVersion" to finalAppVersion,
                "status" to (report.status.ifBlank { "open" }),
                "createdAt" to FieldValue.serverTimestamp(),
                "resolvedAt" to null,
                "adminNotes" to (report.adminNotes.ifBlank { "" })
            )

            bugReportRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getUserBugReports(userId: String): Flow<List<BugReport>> = callbackFlow {
        val listener = firestore.collection("bugReports")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    close()
                    return@addSnapshotListener
                }
                val reports = snapshot?.toObjects(BugReport::class.java) ?: emptyList()
                trySend(reports)
            }
        awaitClose { listener.remove() }
    }
}
