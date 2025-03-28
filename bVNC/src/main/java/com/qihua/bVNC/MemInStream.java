/**
 * Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */
package com.qihua.bVNC;

public class MemInStream extends InStream {

    public MemInStream(byte[] data, int offset, int len) {
        b = data;
        ptr = offset;
        end = offset + len;
    }

    public int pos() {
        return ptr;
    }

    protected int overrun(int itemSize, int nItems) throws Exception {
        throw new Exception("MemInStream overrun: end of stream");
    }
}
