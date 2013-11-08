/*
 * Copyright (c) 2012 adeven GmbH, http://www.adeven.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.adeven.adjustio;

/**
 * @author keyboardsurfer
 * @since 8.11.13
 */
public class Constants {
    protected static final int ONE_SECOND     = 1000;
    protected static final int THIRTY_SECONDS = ONE_SECOND * 30;
    protected static final int ONE_MINUTE     = 60 * ONE_SECOND;

    protected static final String UNKNOWN                   = "unknown";
    protected static final String SESSION_STATE_FILENAME    = "AdjustIoActivityState";
    protected static final String SMALL                     = "small";
    protected static final String NORMAL                    = "normal";
    protected static final String LONG                      = "long";
    protected static final String LARGE                     = "large";
    protected static final String XLARGE                    = "xlarge";
    protected static final String LOW                       = "low";
    protected static final String MEDIUM                    = "medium";
    protected static final String HIGH                      = "high";
    protected static final String NO_ACTIVITY_HANDLER_FOUND = "No activity handler found";
    protected static final String MALFORMED                 = "malformed";
    protected static final String REFERRER                  = "referrer";

    protected static final String ENCODING = "UTF-8";
    protected static final String MD5      = "MD5";
    protected static final String SHA1     = "SHA-1";
}
