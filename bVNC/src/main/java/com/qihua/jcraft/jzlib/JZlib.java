/* -*-mode:java; c-basic-offset:2; -*- */
/*
Copyright (c) 2000,2001,2002,2003 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.qihua.jcraft.jzlib;

final public class JZlib {
    // compression levels
    static final public int Z_NO_COMPRESSION = 0;
    static final public int Z_BEST_SPEED = 1;
    static final public int Z_BEST_COMPRESSION = 9;
    static final public int Z_DEFAULT_COMPRESSION = (-1);
    // compression strategy
    static final public int Z_FILTERED = 1;
    static final public int Z_HUFFMAN_ONLY = 2;
    static final public int Z_DEFAULT_STRATEGY = 0;
    static final public int Z_NO_FLUSH = 0;
    static final public int Z_PARTIAL_FLUSH = 1;
    static final public int Z_SYNC_FLUSH = 2;
    static final public int Z_FULL_FLUSH = 3;
    static final public int Z_FINISH = 4;
    static final public int Z_OK = 0;
    static final public int Z_STREAM_END = 1;
    static final public int Z_NEED_DICT = 2;
    static final public int Z_ERRNO = -1;
    static final public int Z_STREAM_ERROR = -2;
    static final public int Z_DATA_ERROR = -3;
    static final public int Z_MEM_ERROR = -4;
    static final public int Z_BUF_ERROR = -5;
    static final public int Z_VERSION_ERROR = -6;
    private static final String version = "1.0.2";

    public static String version() {
        return version;
    }
}
