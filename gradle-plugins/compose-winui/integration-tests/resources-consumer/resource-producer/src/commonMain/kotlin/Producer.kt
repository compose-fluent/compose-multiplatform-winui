import sample.producer.resources.Res

suspend fun producerMessage(): String = Res.readBytes("files/message.txt").decodeToString().trim()
