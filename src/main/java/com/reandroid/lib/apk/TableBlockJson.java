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
package com.reandroid.lib.apk;

import com.reandroid.lib.arsc.BuildInfo;
import com.reandroid.lib.arsc.chunk.PackageBlock;
import com.reandroid.lib.arsc.chunk.StagedAlias;
import com.reandroid.lib.arsc.chunk.TableBlock;
import com.reandroid.lib.arsc.chunk.TypeBlock;
import com.reandroid.lib.arsc.container.SpecTypePair;
import com.reandroid.lib.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class TableBlockJson {
    private final TableBlock tableBlock;
    public TableBlockJson(TableBlock tableBlock){
        this.tableBlock=tableBlock;
    }
    public void writeJsonFiles(File outDir) throws IOException {
        for(PackageBlock packageBlock: tableBlock.listPackages()){
            writePackageJsonFiles(outDir, packageBlock);
        }
    }
    private void writePackageJsonFiles(File rootDir, PackageBlock packageBlock) throws IOException {
        File pkgDir = new File(rootDir, getDirName(packageBlock));

        writePackageJson(pkgDir, packageBlock);

        for(SpecTypePair specTypePair: packageBlock.listAllSpecTypePair()){
            for(TypeBlock typeBlock:specTypePair.getTypeBlockArray().listItems()){
                writeTypeJsonFiles(pkgDir, typeBlock);
            }
        }
    }
    private void writePackageJson(File packageDirectory, PackageBlock packageBlock) throws IOException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put(BuildInfo.NAME_arsc_lib_version, BuildInfo.getVersion());

        jsonObject.put(PackageBlock.NAME_package_id, packageBlock.getId());
        jsonObject.put(PackageBlock.NAME_package_name, packageBlock.getName());
        StagedAlias stagedAlias=StagedAlias
                .mergeAll(packageBlock.getStagedAliasList().getChildes());
        if(stagedAlias!=null){
            jsonObject.put(PackageBlock.NAME_staged_aliases,
                    stagedAlias.getStagedAliasEntryArray().toJson());
        }

        File packageFile = new File(packageDirectory, PackageBlock.JSON_FILE_NAME);
        jsonObject.write(packageFile);
    }
    private void writeTypeJsonFiles(File packageDirectory, TypeBlock typeBlock) throws IOException {
        File file=new File(packageDirectory,
                getFileName(typeBlock) + ApkUtil.JSON_FILE_EXTENSION);
        JSONObject jsonObject = typeBlock.toJson();
        jsonObject.write(file);
    }
    private String getFileName(TypeBlock typeBlock){
        StringBuilder builder=new StringBuilder();
        builder.append(String.format("0x%02x", typeBlock.getTypeId()));
        String name= typeBlock.getTypeName();
        builder.append('-').append(name);
        builder.append(typeBlock.getResConfig().getQualifiers());
        return builder.toString();
    }
    private String getDirName(PackageBlock packageBlock){
        StringBuilder builder=new StringBuilder();
        builder.append(String.format("0x%02x", packageBlock.getId()));
        builder.append("-");
        builder.append(packageBlock.getIndex());
        String name= packageBlock.getName();
        if(name!=null){
            builder.append('-');
            builder.append(name);
        }
        return builder.toString();
    }
}