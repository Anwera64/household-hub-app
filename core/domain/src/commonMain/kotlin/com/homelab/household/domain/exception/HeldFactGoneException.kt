package com.homelab.household.domain.exception

/**
 * Answering, or undoing, a fact not saved yet that the hub no longer holds open for this member: it
 * expired, was answered (or opened again) on another phone, or isn't theirs. The list is read again.
 */
class HeldFactGoneException : DomainException("This fact is no longer waiting for an answer.")
