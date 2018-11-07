/*
 * Copyright © 1996 - 2017 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors. 
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG. 
*/
/**
 * Trims the leading and trailing spaces from the specified
 * text string.
 * @param	text - the string text
 * @return	the trimmed text
 */
function trimSpace(text) {
	var iStartIndex = 0;
	var iEndIndex = text.length;

	for (var i = 0; i < text.length; ) {
		if (text.charAt(i) == " ") {
			iStartIndex = ++i;
		} else {
			break;
		}
	}
	if (iStartIndex < iEndIndex) {
		for (var i = text.length; i >= 0; ) {
			if (text.charAt(--i) == " ") {
				iEndIndex = i;
			} else {
				break;
			}
		}
	}
//alert(iStartIndex + ", " + iEndIndex);
	return text.substring(iStartIndex, iEndIndex);
}