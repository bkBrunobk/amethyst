package com.vitorpamplona.amethyst.service.model

import com.vitorpamplona.amethyst.model.HexKey
import com.vitorpamplona.amethyst.model.toHexKey
import com.vitorpamplona.amethyst.service.relays.Client
import java.util.Date
import nostr.postr.Utils
import nostr.postr.toHex

class RepostEvent (
  id: HexKey,
  pubKey: HexKey,
  createdAt: Long,
  tags: List<List<String>>,
  content: String,
  sig: HexKey
): Event(id, pubKey, createdAt, kind, tags, content, sig) {


  fun boostedPost() = tags.filter { it.firstOrNull() == "e" }.mapNotNull { it.getOrNull(1) }
  fun originalAuthor() = tags.filter { it.firstOrNull() == "p" }.mapNotNull { it.getOrNull(1) }
  fun taggedAddresses() = tags.filter { it.firstOrNull() == "a" }.mapNotNull { it.getOrNull(1) }.mapNotNull { ATag.parse(it) }

  fun containedPost() = try {
    fromJson(content, Client.lenient)
  } catch (e: Exception) {
    null
  }

  companion object {
    const val kind = 6

    fun create(boostedPost: Event, privateKey: ByteArray, createdAt: Long = Date().time / 1000): RepostEvent {
      val content = boostedPost.toJson()

      val replyToPost = listOf("e", boostedPost.id)
      val replyToAuthor = listOf("p", boostedPost.pubKey)

      val pubKey = Utils.pubkeyCreate(privateKey).toHexKey()
      var tags:List<List<String>> = boostedPost.tags.plus(listOf(replyToPost, replyToAuthor))

      if (boostedPost is LongTextNoteEvent) {
        tags = tags + listOf( listOf("a", boostedPost.address().toTag()) )
      }

      val id = generateId(pubKey, createdAt, kind, tags, content)
      val sig = Utils.sign(id, privateKey)
      return RepostEvent(id.toHexKey(), pubKey, createdAt, tags, content, sig.toHexKey())
    }
  }
}