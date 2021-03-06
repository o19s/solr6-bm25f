/**
 *  Copyright 2012 Diego Ceccarelli
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.lucene.search;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.similarities.BM25FSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;

public class TestBM25FQuery extends LuceneTestCase {
  
  IndexSearcher searcherUnderTest;
  RandomIndexWriter indexWriterUnderTest;
  IndexReader indexReaderUnderTest;
  Directory dirUnderTest;

  final BM25FParameters bm25FParameters = new BM25FParameters();
  
  @Before
  public void setupIndex() throws IOException {
    dirUnderTest = newDirectory();
    
    indexWriterUnderTest = new RandomIndexWriter(random(), dirUnderTest);
    Document doc = new Document();
    doc.add(newStringField("id", "0", Store.YES));
    doc.add(newTextField("title", "leonardo da vinci", Store.YES));
    doc.add(newTextField("author", "leonardo da", Store.YES));
    doc.add(newTextField("description", "VIDEO", Store.YES));
    
    indexWriterUnderTest.addDocument(doc);

    doc = new Document();
    doc.add(newStringField("id", "1", Store.YES));
    doc.add(newTextField("title", "leonardo ", Store.YES));
    doc.add(newTextField("author", "leonardo da vinci ", Store.YES));
    doc.add(newTextField("description", "IMAGE", Store.YES));
    
    indexWriterUnderTest.addDocument(doc);

    doc = new Document();
    doc.add(newStringField("id", "2", Store.YES));
    doc.add(newTextField("title", "leonardo da vinci", Store.YES));
    doc.add(newTextField("author", "leonardo da", Store.YES));
    doc.add(newTextField("description", "VIDEO", Store.YES));
    
    indexWriterUnderTest.addDocument(doc);

    indexWriterUnderTest.commit();
    indexReaderUnderTest = indexWriterUnderTest.getReader();
    searcherUnderTest = newSearcher(indexReaderUnderTest);

    bm25FParameters.setK1(1);
    bm25FParameters.addFieldParams("title", 1,1);
    bm25FParameters.addFieldParams("author", 1,1);
    bm25FParameters.addFieldParams("description", 1,1);
    bm25FParameters.setMainField("title");
    searcherUnderTest.setSimilarity(new BM25FSimilarity(bm25FParameters));

  }
  
  @After
  public void closeStuff() throws IOException {
    indexReaderUnderTest.close();
    indexWriterUnderTest.close();
    dirUnderTest.close();
    
  }

  
  public ScoreDoc[] getBM25FResults(String field, String query) throws IOException {

    final Query q = new BM25FBooleanTermQuery(new Term(field,query), bm25FParameters);

    // we should get the "brown" docs backwards first (ie the nworb)
    final TopDocs t = searcherUnderTest.search(q, 10);
    final ScoreDoc[] docs = t.scoreDocs;
    return docs;
  }
  
  @Test
  public void testSearch() throws IOException, QueryNodeException {
    // we should get the "brown" docs backwards first (ie the nworb)
    final ScoreDoc[] docs = getBM25FResults("title","leonardo");
    assertEquals(3, docs.length);
  }


  @Test
  public void testExplainMatchScore() throws IOException{
    // we should get an explain of a main score and sub scores per term
    final Query q = new BM25FBooleanTermQuery(new Term("title","leonardo"), bm25FParameters);

    final ScoreDoc[] docs = getBM25FResults("title","leonardo");
    for(ScoreDoc doc: docs){
      //System.out.println(searcherUnderTest.explain(q, doc.doc));
      assertEquals(doc.score, searcherUnderTest.explain(q, doc.doc).getValue(), 0.0001);
    }
  }
  
}
