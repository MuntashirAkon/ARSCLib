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
package com.reandroid.dex.ins;

import com.reandroid.dex.base.Ule128Item;
import com.reandroid.dex.id.TypeId;
import com.reandroid.dex.data.DexContainerItem;
import com.reandroid.dex.data.InstructionList;
import com.reandroid.dex.writer.SmaliWriter;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.collection.ArrayIterator;
import com.reandroid.utils.collection.EmptyIterator;

import java.io.IOException;
import java.util.Iterator;

public abstract class ExceptionHandler extends DexContainerItem
        implements Iterable<Label>, LabelsSet {

    private final Ule128Item catchAddress;

    private final ExceptionLabel startLabel;
    private final ExceptionLabel endLabel;
    private final ExceptionLabel handlerLabel;
    private final ExceptionLabel catchLabel;

    private final Label[] mLabels;


    private ExceptionHandler(int childesCount, Ule128Item catchAddress, int index) {
        super(childesCount);
        this.catchAddress = catchAddress;
        if(catchAddress != null){
            addChild(index, catchAddress);
        }

        this.startLabel = new TryStartLabel(this);
        this.endLabel = new TryEndLabel(this);
        this.handlerLabel = new HandlerLabel(this);
        this.catchLabel = new CatchLabel(this);

        this.mLabels = new Label[]{this.startLabel, this.endLabel, this.handlerLabel, this.catchLabel};
    }
    ExceptionHandler(int childesCount) {
        this(childesCount + 1, new Ule128Item(), childesCount);
    }

    ExceptionHandler() {
        this(0, null, 0);
    }


    public Iterator<Ins> getTryInstructions(){
        InstructionList instructionList = getInstructionList();
        if(instructionList == null){
            return EmptyIterator.of();
        }
        return instructionList.iteratorByAddress(
                getStartLabel().getTargetAddress(), getCodeUnit());
    }
    private InstructionList getInstructionList(){
        TryItem tryItem = getTryItem();
        if(tryItem != null){
            return tryItem.getInstructionList();
        }
        return null;
    }

    abstract TypeId getTypeId();
    abstract String getOpcodeName();
    Ule128Item getCatchAddressUle128(){
        return catchAddress;
    }

    @Override
    public Iterator<Label> getLabels(){
        return iterator();
    }
    @Override
    public Iterator<Label> iterator(){
        return ArrayIterator.of(mLabels);
    }
    public ExceptionLabel getHandlerLabel(){
        return handlerLabel;
    }
    public ExceptionLabel getStartLabel(){
        return startLabel;
    }
    public ExceptionLabel getEndLabel(){
        return endLabel;
    }
    public ExceptionLabel getCatchLabel(){
        return catchLabel;
    }

    public int getCatchAddress(){
        return getCatchAddressUle128().get();
    }
    public void setCatchAddress(int address){
        getCatchAddressUle128().set(address);
    }
    public int getAddress(){
        return getStartAddress() + getCodeUnit();
    }
    public void setAddress(int address){
        setCodeUnit(address - getStartAddress());
    }

    public int getStartAddress(){
        TryItem tryItem = getTryItem();
        if(tryItem != null){
            return tryItem.getStartAddress();
        }
        return 0;
    }
    public void setStartAddress(int address){
        TryItem tryItem = getTryItem();
        if(tryItem != null){
            tryItem.setStartAddress(address);
        }
    }
    public int getCodeUnit(){
        TryItem tryItem = getTryItem();
        if(tryItem != null){
            return tryItem.getCatchCodeUnit();
        }
        return 0;
    }
    public void setCodeUnit(int value){
        TryItem tryItem = getTryItem();
        if(tryItem != null){
            tryItem.getHandlerOffset().setCatchCodeUnit(value);
        }
    }
    TryItem getTryItem(){
        return getParentInstance(TryItem.class);
    }

    @Override
    public String toString() {
        return getHandlerLabel().toString();
    }

    public static class HandlerLabel implements ExceptionLabel{

        private final ExceptionHandler handler;

        HandlerLabel(ExceptionHandler handler){
            this.handler = handler;
        }

        @Override
        public ExceptionHandler getHandler() {
            return handler;
        }

        @Override
        public int getAddress(){
            return getHandler().getAddress();
        }
        @Override
        public int getTargetAddress() {
            return getHandler().getAddress();
        }
        @Override
        public void setTargetAddress(int targetAddress){
            getHandler().setAddress(targetAddress);
        }

        @Override
        public String getLabelName() {
            ExceptionHandler handler = this.handler;
            StringBuilder builder = new StringBuilder();
            builder.append('.');
            builder.append(handler.getOpcodeName());
            builder.append(' ');
            TypeId typeId = handler.getTypeId();
            if(typeId != null){
                builder.append(typeId.getName());
                builder.append(' ');
            }
            builder.append("{");
            builder.append(handler.getStartLabel().getLabelName());
            builder.append(" .. ");
            builder.append(handler.getEndLabel().getLabelName());
            builder.append("} ");
            builder.append(handler.getCatchLabel().getLabelName());
            return builder.toString();
        }

        @Override
        public int getSortOrder() {
            return ExtraLine.ORDER_EXCEPTION_HANDLER;
        }
        @Override
        public boolean isEqualExtraLine(Object obj) {
            if(obj == this){
                return true;
            }
            if(obj == null || this.getClass() != obj.getClass()){
                return false;
            }
            HandlerLabel label = (HandlerLabel) obj;
            return this.handler == label.handler;
        }
        @Override
        public void appendExtra(SmaliWriter writer) throws IOException {
            ExceptionHandler handler = this.handler;
            writer.append('.');
            writer.append(handler.getOpcodeName());
            writer.append(' ');
            TypeId typeId = handler.getTypeId();
            if(typeId != null){
                typeId.append(writer);
                writer.append(' ');
            }
            writer.append("{");
            writer.append(handler.getStartLabel().getLabelName());
            writer.append(" .. ");
            writer.append(handler.getEndLabel().getLabelName());
            writer.append("} ");
            writer.append(handler.getCatchLabel().getLabelName());
        }
        @Override
        public String toString() {
            return getLabelName();
        }
    }

    public static class TryStartLabel implements ExceptionLabel{

        private final ExceptionHandler handler;

        TryStartLabel(ExceptionHandler handler){
            this.handler = handler;
        }

        @Override
        public ExceptionHandler getHandler() {
            return handler;
        }

        @Override
        public int getAddress(){
            return getHandler().getAddress();
        }
        @Override
        public int getTargetAddress() {
            return getHandler().getStartAddress();
        }
        @Override
        public void setTargetAddress(int targetAddress){
            getHandler().setStartAddress(targetAddress);
        }
        @Override
        public String getLabelName() {
            return HexUtil.toHex(":try_start_", getTargetAddress(), 1);
        }

        @Override
        public int getSortOrder() {
            return ExtraLine.ORDER_TRY_START;
        }

        @Override
        public boolean isEqualExtraLine(Object obj) {
            if(obj == this){
                return true;
            }
            if(obj == null || this.getClass() != obj.getClass()){
                return false;
            }
            TryStartLabel label = (TryStartLabel) obj;
            if(handler == label.handler){
                return true;
            }
            return getTargetAddress() == label.getTargetAddress();
        }
        @Override
        public String toString() {
            return getLabelName();
        }
    }

    public static class TryEndLabel implements ExceptionLabel{

        private final ExceptionHandler handler;

        TryEndLabel(ExceptionHandler handler){
            this.handler = handler;
        }

        @Override
        public ExceptionHandler getHandler() {
            return handler;
        }

        @Override
        public int getAddress() {
            return getHandler().getAddress();
        }
        @Override
        public int getTargetAddress() {
            return getHandler().getAddress();
        }
        @Override
        public void setTargetAddress(int targetAddress){
            getHandler().setAddress(targetAddress);
        }
        @Override
        public String getLabelName() {
            return HexUtil.toHex(":try_end_", getTargetAddress(), 1);
        }

        @Override
        public int getSortOrder() {
            return ExtraLine.ORDER_TRY_END;
        }
        @Override
        public boolean isEqualExtraLine(Object obj) {
            if(obj == this){
                return true;
            }
            if(obj == null || this.getClass() != obj.getClass()){
                return false;
            }
            TryEndLabel label = (TryEndLabel) obj;
            if(handler == label.handler){
                return true;
            }
            return getTargetAddress() == label.getTargetAddress();
        }
        @Override
        public String toString() {
            return getLabelName();
        }
    }

    public static class CatchLabel implements ExceptionLabel{

        private final ExceptionHandler handler;

        CatchLabel(ExceptionHandler handler){
            this.handler = handler;
        }

        @Override
        public ExceptionHandler getHandler() {
            return handler;
        }

        @Override
        public int getAddress() {
            return getHandler().getStartLabel().getAddress();
        }
        @Override
        public int getTargetAddress() {
            return getHandler().getCatchAddress();
        }
        @Override
        public void setTargetAddress(int targetAddress){
            getHandler().setCatchAddress(targetAddress);
        }
        @Override
        public String getLabelName() {
            return HexUtil.toHex(":" + getHandler().getOpcodeName() + "_", getTargetAddress(), 1);
        }
        @Override
        public int getSortOrder() {
            return ExtraLine.ORDER_CATCH;
        }
        @Override
        public boolean isEqualExtraLine(Object obj) {
            if(obj == this){
                return true;
            }
            if(obj == null || this.getClass() != obj.getClass()){
                return false;
            }
            CatchLabel label = (CatchLabel) obj;
            if(handler == label.handler){
                return true;
            }
            return getTargetAddress() == label.getTargetAddress();
        }
        @Override
        public String toString() {
            return getLabelName();
        }
    }
}