package com.tyro.oss.rabbit_amazon_bridge.messagetransformer

class DoNothingMessageTransformer : MessageTransformer {
    override fun transform(message: String) = message
}