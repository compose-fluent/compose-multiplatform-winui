package org.jetbrains.compose.resources

import org.jetbrains.compose.resources.vector.xmldom.Element
import org.jetbrains.compose.resources.vector.xmldom.parseWindowsXml

internal actual fun ByteArray.toXmlElement(): Element = parseWindowsXml(decodeToString())
