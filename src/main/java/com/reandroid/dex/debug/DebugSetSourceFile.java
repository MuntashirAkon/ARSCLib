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

import com.reandroid.dex.id.StringId;
import com.reandroid.dex.key.StringKey;
import com.reandroid.dex.reference.Base1Ule128IdItemReference;
import com.reandroid.dex.sections.SectionType;

public class DebugSetSourceFile extends DebugElement {

    private final Base1Ule128IdItemReference<StringId> mName;

    public DebugSetSourceFile() {
        super(1, DebugElementType.SET_SOURCE_FILE);
        this.mName = new Base1Ule128IdItemReference<>(SectionType.STRING_ID);
        addChild(1, mName);
    }

    public String getName(){
        StringId stringId = mName.getItem();
        if(stringId != null){
            return stringId.getString();
        }
        return null;
    }
    public StringKey getNameKey(){
        return (StringKey) mName.getKey();
    }
    public void setName(String name){
        mName.setItem(StringKey.create(name));
    }
    public void setName(StringKey key){
        mName.setItem(key);
    }

    @Override
    public DebugElementType<DebugSetSourceFile> getElementType() {
        return DebugElementType.SET_SOURCE_FILE;
    }

    @Override
    public String toString() {
        return super.toString() + ", " + mName;
    }
}