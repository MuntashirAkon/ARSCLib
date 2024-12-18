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
package com.reandroid.dex.smali.model;

import com.reandroid.dex.ins.InsArrayData;
import com.reandroid.dex.ins.Opcode;
import com.reandroid.dex.key.PrimitiveKey;
import com.reandroid.dex.smali.*;

import java.io.IOException;
import java.util.Iterator;

public class SmaliPayloadArray extends SmaliInstructionPayload<SmaliValueNumber<?>> {

    public SmaliPayloadArray(){
        super(new SmaliInstructionOperand.SmaliDecimalOperand());
    }

    public void addEntryKeys(Iterator<PrimitiveKey> iterator) {
        while (iterator.hasNext()) {
            addEntry(iterator.next());
        }
    }
    public void addEntry(PrimitiveKey key) {
        SmaliValueNumber<?> valueNumber = SmaliValueFactory.valueNumberFor(key);
        addEntry(valueNumber);
        valueNumber.setKey(key);
    }
    public int[] unsignedInt(){
        SmaliSet<SmaliValueNumber<?>> entries = getEntries();
        int size = entries.size();
        int[] result = new int[size];
        for(int i = 0; i < size; i++){
            result[i] = entries.get(i).unsignedInt();
        }
        return result;
    }
    public long[] unsignedLong(){
        SmaliSet<SmaliValueNumber<?>> entries = getEntries();
        int size = entries.size();
        long[] result = new long[size];
        for(int i = 0; i < size; i++){
            result[i] = entries.get(i).unsignedLong();
        }
        return result;
    }
    @Override
    public SmaliInstructionOperand.SmaliDecimalOperand getOperand() {
        return (SmaliInstructionOperand.SmaliDecimalOperand) super.getOperand();
    }

    public int getWidth() {
        return getOperand().getNumber();
    }
    public void setWidth(int width) {
        getOperand().setNumber(width);
    }

    @Override
    public int getCodeUnits() {
        int count = getCount();
        int width = getWidth();

        int size = 2 // opcode bytes
                + 2  // width short reference
                + 4  // count integer reference
                + width * count;

        int align = (2 - (size % 2)) % 2;

        size += align;
        return size / 2;
    }
    @Override
    public SmaliDirective getSmaliDirective() {
        return SmaliDirective.ARRAY_DATA;
    }
    @Override
    public Opcode<InsArrayData> getOpcode() {
        return Opcode.ARRAY_PAYLOAD;
    }
    @Override
    SmaliValueNumber<?> createEntry(SmaliReader reader) throws IOException {
        SmaliValue value =  SmaliValueFactory.create(reader);
        if(!(value instanceof SmaliValueNumber)){
            throw new SmaliParseException("Unrecognized array data entry", reader);
        }
        SmaliValueNumber<?> valueNumber = (SmaliValueNumber<?>) value;
        if(valueNumber.getWidth() > getWidth()){
            throw new SmaliParseException("Value out of range", reader);
        }
        return valueNumber;
    }

    @Override
    void parseOperand(Opcode<?> opcode, SmaliReader reader) throws IOException {
        reader.skipWhitespacesOrComment();
        int position = reader.position();
        SmaliInstructionOperand.SmaliDecimalOperand operand = getOperand();
        operand.parse(opcode, reader);
        int width = operand.getNumber();

        //TODO: validate if width == 0, then the size (entries count) also must be zero.
        // .array-data 0
        // .end-array-data

        if(width < 0 || width > 8) {
            reader.position(position);
            throw new SmaliParseException("Array width out of range (0 .. 8) : '" + width + "'", reader);
        }
    }
}
