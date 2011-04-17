/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.mapper.xcontent;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.Term;
import org.elasticsearch.common.lucene.Lucene;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.mapper.MergeMappingException;

import java.io.IOException;
import java.util.ArrayDeque;

/**
 * @author kimchy (shay.banon)
 */
public class TypeFieldMapper extends AbstractFieldMapper<String> implements org.elasticsearch.index.mapper.TypeFieldMapper {

    public static final String CONTENT_TYPE = "_type";

    public static class Defaults extends AbstractFieldMapper.Defaults {
        public static final String NAME = org.elasticsearch.index.mapper.TypeFieldMapper.NAME;
        public static final String INDEX_NAME = org.elasticsearch.index.mapper.TypeFieldMapper.NAME;
        public static final Field.Index INDEX = Field.Index.NOT_ANALYZED;
        public static final Field.Store STORE = Field.Store.NO;
        public static final boolean OMIT_NORMS = true;
        public static final boolean OMIT_TERM_FREQ_AND_POSITIONS = true;
    }

    public static class Builder extends AbstractFieldMapper.Builder<Builder, TypeFieldMapper> {

        public Builder() {
            super(Defaults.NAME);
            indexName = Defaults.INDEX_NAME;
            index = Defaults.INDEX;
            store = Defaults.STORE;
            omitNorms = Defaults.OMIT_NORMS;
            omitTermFreqAndPositions = Defaults.OMIT_TERM_FREQ_AND_POSITIONS;
        }

        @Override public TypeFieldMapper build(BuilderContext context) {
            return new TypeFieldMapper(name, indexName, store, termVector, boost, omitNorms, omitTermFreqAndPositions);
        }
    }

    private final ThreadLocal<ArrayDeque<Field>> fieldCache = new ThreadLocal<ArrayDeque<Field>>() {
        @Override protected ArrayDeque<Field> initialValue() {
            return new ArrayDeque<Field>();
        }
    };

    protected TypeFieldMapper() {
        this(Defaults.NAME, Defaults.INDEX_NAME);
    }

    protected TypeFieldMapper(String name, String indexName) {
        this(name, indexName, Defaults.STORE, Defaults.TERM_VECTOR, Defaults.BOOST,
                Defaults.OMIT_NORMS, Defaults.OMIT_TERM_FREQ_AND_POSITIONS);
    }

    public TypeFieldMapper(String name, String indexName, Field.Store store, Field.TermVector termVector,
                           float boost, boolean omitNorms, boolean omitTermFreqAndPositions) {
        super(new Names(name, indexName, indexName, name), Defaults.INDEX, store, termVector, boost, omitNorms, omitTermFreqAndPositions,
                Lucene.KEYWORD_ANALYZER, Lucene.KEYWORD_ANALYZER);
    }

    @Override public String value(Document document) {
        Fieldable field = document.getFieldable(names.indexName());
        return field == null ? null : value(field);
    }

    @Override public String value(Fieldable field) {
        return field.stringValue();
    }

    @Override public String valueFromString(String value) {
        return value;
    }

    @Override public String valueAsString(Fieldable field) {
        return value(field);
    }

    @Override public String indexedValue(String value) {
        return value;
    }

    @Override public Term term(String value) {
        return new Term(names.indexName(), value);
    }

    @Override protected Field parseCreateField(ParseContext context) throws IOException {
        ArrayDeque<Field> cache = fieldCache.get();
        Field field = cache.poll();
        if (field == null) {
            field = new Field(names.indexName(), "", store, index);
        }
        field.setValue(context.type());
        return field;
    }

    @Override public void processFieldAfterIndex(Fieldable field) {
        fieldCache.get().add((Field) field);
    }

    @Override public void close() {
        fieldCache.remove();
    }

    @Override protected String contentType() {
        return CONTENT_TYPE;
    }

    @Override public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        // if all are defaults, no sense to write it at all
        if (store == Defaults.STORE) {
            return builder;
        }
        builder.startObject(CONTENT_TYPE);
        if (store != Defaults.STORE) {
            builder.field("store", store.name().toLowerCase());
        }
        builder.endObject();
        return builder;
    }

    @Override public void merge(XContentMapper mergeWith, MergeContext mergeContext) throws MergeMappingException {
        // do nothing here, no merging, but also no exception
    }
}
