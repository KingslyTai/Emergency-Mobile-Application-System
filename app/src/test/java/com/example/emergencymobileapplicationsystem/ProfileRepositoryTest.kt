// ProfileRepositoryTest.kt
package com.example.emergencymobileapplicationsystem.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

class ProfileRepositoryTest {

    private lateinit var profileRepository: ProfileRepository
    private lateinit var firestore: FirebaseFirestore
    private lateinit var profilesCollection: CollectionReference
    private lateinit var documentReference: DocumentReference

    @Before
    fun setUp() {
        firestore = mock() // Mock FirebaseFirestore
        profilesCollection = mock() // Mock the profiles collection
        documentReference = mock() // Mock a document reference

        // Mock the "profiles" collection reference in Firestore
        `when`(firestore.collection("profiles")).thenReturn(profilesCollection)
        `when`(profilesCollection.document(any())).thenReturn(documentReference)

        // Initialize ProfileRepository with the mocked Firestore
        profileRepository = ProfileRepository(firestore)
    }

    @Test
    fun `test createProfile succeeds`() = runBlocking {
        // Arrange: Mock successful completion of the set operation
        `when`(documentReference.set(any())).thenReturn(mock())

        // Data to be used for profile creation
        val profileData = mapOf("name" to "John Doe", "age" to "30")

        // Act: Call createProfile in the repository
        val result = profileRepository.createProfile("user123", profileData)

        // Assert: Verify that the profile was created successfully
        assertTrue(result)

        // Verify that the set operation was called on the correct document reference
        verify(documentReference).set(profileData)
    }
}
