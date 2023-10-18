/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.dex.debug;

import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.item.ByteItem;
import com.reandroid.dex.base.DexException;
import com.reandroid.dex.ins.ExtraLine;
import com.reandroid.dex.data.DexContainerItem;
import com.reandroid.dex.writer.SmaliWriter;

import java.io.IOException;

public abstract class DebugElement extends DexContainerItem implements ExtraLine {
    private final ByteItem elementType;
    private int address;
    private int lineNumber;

    DebugElement(int childesCount, int flag) {
        super(childesCount + 1);

        this.elementType = new ByteItem();
        this.elementType.set((byte) flag);

        addChild(0, elementType);
    }
    DebugElement(int childesCount, DebugElementType<?> elementType) {
        this(childesCount, elementType.getFlag());
    }
    DebugElement(DebugElementType<?> elementType) {
        this(0, elementType.getFlag());
    }

    public void removeSelf(){
        DebugSequence debugSequence = getDebugSequence();
        if(debugSequence != null){
            debugSequence.remove(this);
        }
    }

    int getAddressDiff(){
        return 0;
    }
    void setAddressDiff(int diff){
    }
    int getLineDiff(){
        return 0;
    }
    void setLineDiff(int diff){
    }

    DebugSequence getDebugSequence(){
        return getParent(DebugSequence.class);
    }

    @Override
    public int getTargetAddress() {
        return address;
    }
    @Override
    public void setTargetAddress(int address) {
        if(address == getTargetAddress()){
            return;
        }
        DebugSequence sequence = getDebugSequence();
        if(sequence == null){
            return;
        }
        DebugElement previous = sequence.get(getIndex() - 1);
        int diff;
        if(previous == null){
            diff = 0;
        }else {
            diff = address - previous.getTargetAddress();
        }
        setAddressDiff(diff);
        this.address = address;
        DebugElement next = sequence.get(getIndex() + 1);
        if(next != null){
            next.setTargetAddress(address + next.getAddressDiff());
        }
    }
    int getLineNumber(){
        return lineNumber;
    }
    void setLineNumber(int lineNumber){
        this.lineNumber = lineNumber;
    }

    int getFlag(){
        int flag = elementType.unsignedInt();
        if(flag > 0x0A){
            flag = 0x0A;
        }
        return flag;
    }
    int getFlagOffset(){
        int offset = elementType.unsignedInt();
        if(offset < 0x0A){
            return 0;
        }
        return offset - 0x0A;
    }
    void setFlagOffset(int offset){
        int flag = getFlag();
        if(flag < 0x0A){
            if(offset == 0){
                return;
            }
            throw new IllegalArgumentException("Can not set offset for: " + getElementType());
        }
        if(offset < 0 || offset > 0xF5){
            throw new DexException("Value out of range should be [0 - 245]: " + offset + ", prev = " + getFlagOffset());
        }
        int value = flag + offset;
        elementType.set((byte) value);
    }
    public abstract DebugElementType<?> getElementType();
    void cacheValues(DebugSequence debugSequence, DebugElement previous){
        int line;
        int address;
        if(previous == null){
            address = 0;
            line = debugSequence.getLineStart();
        }else {
            address = previous.getTargetAddress();
            line = previous.getLineNumber();
        }
        address += getAddressDiff();
        line += getLineDiff();
        this.address = address;
        this.lineNumber = line;
    }
    void updateValues(DebugSequence debugSequence, DebugElement previous){
        if(previous == this){
            return;
        }
        if(previous != null && previous.getParent() == null){
            return;
        }
        int line;
        int address;
        if(previous == null){
            address = 0;
            line = debugSequence.getLineStart();
        }else {
            address = previous.getTargetAddress();
            line = previous.getLineNumber();
        }
        int addressDiff = getTargetAddress() - address;
        int lineDiff = getLineNumber() - line;
        setAddressDiff(addressDiff);
        setLineDiff(lineDiff);
    }

    @Override
    public void onReadBytes(BlockReader reader) throws IOException {
        super.nonCheckRead(reader);
    }

    @Override
    public void appendExtra(SmaliWriter writer) throws IOException {
        writer.append(getElementType().getOpcode());
    }
    @Override
    public boolean isEqualExtraLine(Object obj) {
        return obj == this;
    }
    @Override
    public int getSortOrder() {
        return ExtraLine.ORDER_DEBUG_LINE;
    }
    @Override
    public String toString() {
        return "Type = " + getElementType();
    }
}