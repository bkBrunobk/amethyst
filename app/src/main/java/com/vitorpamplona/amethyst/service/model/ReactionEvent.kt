package com.vitorpamplona.amethyst.service.model

import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.toHexKey
import java.util.Date
import nostr.postr.Utils
import nostr.postr.toHex

class ReactionEvent (
  id: HexKey,
  pubKey: HexKey,
  createdAt: Long,
  tags: List<List<String>>,
  content: String,
  sig: HexKey
): Event(id, pubKey, createdAt, kind, tags, content, sig) {

  fun originalPost() = tags.filter { it.firstOrNull() == "e" }.mapNotNull { it.getOrNull(1) }
  fun originalAuthor() = tags.filter { it.firstOrNull() == "p" }.mapNotNull { it.getOrNull(1) }
  fun taggedAddresses() = tags.filter { it.firstOrNull() == "a" }.mapNotNull { it.getOrNull(1) }.mapNotNull { ATag.parse(it) }

  companion object {
    const val kind = 7

    fun createWarning(originalNote: Event, privateKey: ByteArray, createdAt: Long = Date().time / 1000): ReactionEvent {
      return create("\u26A0\uFE0F", originalNote, privateKey, createdAt)
    }

    fun createLike(originalNote: Event, privateKey: ByteArray, createdAt: Long = Date().time / 1000): ReactionEvent {
      return create("+", originalNote, privateKey, createdAt)
    }

    fun create(content: String, originalNote: Event, privateKey: ByteArray, createdAt: Long = Date().time / 1000): ReactionEvent {
      val pubKey = Utils.pubkeyCreate(privateKey).toHexKey()

      var tags = listOf( listOf("e", originalNote.id), listOf("p", originalNote.pubKey))
      if (originalNote is LongTextNoteEvent) {
        tags = tags + listOf( listOf("a", originalNote.address().toTag()) )
      }

      val id = generateId(pubKey, createdAt, kind, tags, content)
      val sig = Utils.sign(id, privateKey)
      return ReactionEvent(id.toHexKey(), pubKey, createdAt, tags, content, sig.toHexKey())
    }
  }
}