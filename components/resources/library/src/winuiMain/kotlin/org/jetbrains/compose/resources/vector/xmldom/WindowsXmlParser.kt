package org.jetbrains.compose.resources.vector.xmldom

/** Small namespace-aware DOM reader for Android vector resources; DTDs are not supported. */
internal fun parseWindowsXml(xml: String): Element = WindowsXmlParser(xml.removePrefix("\uFEFF")).parse()

private class WindowsElement(
    override val nodeName: String,
    private val attributes: Map<String, String>,
    private val namespaces: Map<String, String>
) : Element {
    val children = mutableListOf<WindowsElement>()
    val text = StringBuilder()
    override val textContent get() = text.toString()
    override val localName get() = nodeName.substringAfter(':')
    override val namespaceURI get() = namespaces[nodeName.substringBefore(':', "")].orEmpty()
    override val childNodes = object : NodeList {
        override val length get() = children.size
        override fun item(i: Int): Node = children[i]
    }
    override fun lookupPrefix(namespaceURI: String) = namespaces.entries.firstOrNull { it.value == namespaceURI }?.key.orEmpty()
    override fun getAttribute(name: String) = attributes[name].orEmpty()
    override fun getAttributeNS(nameSpaceURI: String, localName: String): String =
        attributes.entries.firstOrNull { (name, _) ->
            name.substringAfter(':') == localName &&
                (if (':' in name) namespaces[name.substringBefore(':')].orEmpty() else "") == nameSpaceURI
        }?.value.orEmpty()
}

private class WindowsXmlParser(private val xml: String) {
    private var position = 0
    private fun fail(): Nothing = throw MalformedXMLException("Malformed vector XML at character $position")
    private fun starts(value: String) = xml.startsWith(value, position)
    private fun consume(value: String) { if (!starts(value)) fail(); position += value.length }
    private fun whitespace() { while (position < xml.length && xml[position].isWhitespace()) position++ }
    private fun name(): String {
        val begin = position
        while (position < xml.length && (xml[position].isLetterOrDigit() || xml[position] in "_:.-")) position++
        if (begin == position || !(xml[begin].isLetter() || xml[begin] in "_:")) fail()
        return xml.substring(begin, position)
    }
    private fun until(delimiter: String): String {
        val end = xml.indexOf(delimiter, position)
        if (end < 0) fail()
        return xml.substring(position, end).also { position = end + delimiter.length }
    }
    private fun trivia() {
        while (true) {
            whitespace()
            when {
                starts("<?") -> { position += 2; until("?>") }
                starts("<!--") -> { position += 4; until("-->") }
                else -> return
            }
        }
    }
    fun parse(): Element {
        trivia()
        val root = element(mapOf("xml" to "http://www.w3.org/XML/1998/namespace"), 0)
        trivia()
        if (position != xml.length) fail()
        return root
    }
    private fun element(parentNamespaces: Map<String, String>, depth: Int): WindowsElement {
        if (depth > 256) fail()
        consume("<")
        val tag = name()
        val attributes = mutableMapOf<String, String>()
        while (true) {
            val before = position
            whitespace()
            if (starts("/>") || starts(">")) break
            if (before == position) fail()
            val key = name()
            whitespace(); consume("="); whitespace()
            val quote = xml.getOrNull(position++) ?: fail()
            if (quote != '\'' && quote != '"') fail()
            val value = until(quote.toString())
            if ('<' in value || attributes.put(key, entities(value)) != null) fail()
        }
        val namespaces = parentNamespaces.toMutableMap()
        attributes.forEach { (key, value) ->
            if (key == "xmlns") namespaces[""] = value
            else if (key.startsWith("xmlns:")) namespaces[key.substringAfter(':')] = value
        }
        if (':' in tag && tag.substringBefore(':') !in namespaces) fail()
        val result = WindowsElement(tag, attributes, namespaces)
        if (starts("/>")) { position += 2; return result }
        consume(">")
        while (!starts("</")) {
            when {
                position >= xml.length -> fail()
                starts("<!--") -> { position += 4; until("-->") }
                starts("<?") -> { position += 2; until("?>") }
                starts("<![CDATA[") -> { position += 9; result.text.append(until("]]>")) }
                starts("<") -> result.children.add(element(namespaces, depth + 1))
                else -> {
                    val end = xml.indexOf('<', position)
                    if (end < 0) fail()
                    result.text.append(entities(xml.substring(position, end)))
                    position = end
                }
            }
        }
        consume("</")
        if (name() != tag) fail()
        whitespace(); consume(">")
        return result
    }
    private fun entities(value: String): String = buildString {
        var cursor = 0
        while (cursor < value.length) {
            if (value[cursor] != '&') { append(value[cursor++]); continue }
            val end = value.indexOf(';', cursor)
            if (end < 0) fail()
            val entity = value.substring(cursor + 1, end)
            val code = when (entity) {
                "amp" -> 38; "lt" -> 60; "gt" -> 62; "quot" -> 34; "apos" -> 39
                else -> when {
                    entity.startsWith("#x") -> entity.drop(2).toIntOrNull(16)
                    entity.startsWith('#') -> entity.drop(1).toIntOrNull()
                    else -> null
                } ?: fail()
            }
            if (code !in 1..0x10ffff || code in 0xd800..0xdfff) fail()
            if (code <= 0xffff) append(code.toChar()) else {
                append(((code - 0x10000) / 1024 + 0xd800).toChar())
                append(((code - 0x10000) % 1024 + 0xdc00).toChar())
            }
            cursor = end + 1
        }
    }
}
