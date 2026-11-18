package com.example.daysmark.core.domain.repository

/**
 * The device's storage couldn't be read or written (the database couldn't be opened, a disk was
 * full, a stored row was damaged). The data layer turns every such failure into this, keeping the
 * original as [cause]; anything else a repository throws is a programmer error and is not caught.
 */
public class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
