/*
 * Anserini: A Lucene toolkit for reproducible information retrieval research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.anserini.search.query;

import io.anserini.analysis.AnalyzerUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Query generator that uses dismax queries to search over multiple fields. Similarly
 * to {@link BagOfWordsQueryGenerator} it models the query text as a 'bag of terms'.
 */
public class DisjunctionMaxQueryGenerator extends QueryGenerator {
  private final float tiebreaker;

  public DisjunctionMaxQueryGenerator(float tiebreaker) {
    this.tiebreaker = tiebreaker;
  }

  @Override
  public Query buildQuery(String field, Analyzer analyzer, String queryText) {
    List<String> tokens = AnalyzerUtils.analyze(analyzer, queryText);
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (String t : tokens) {
      builder.add(new TermQuery(new Term(field, t)), BooleanClause.Occur.SHOULD);
    }
    return builder.build();
  }

  @Override
  public Query buildQuery(Map<String, Float> fields, Analyzer analyzer, String queryText) {
    List<Query> clauses = new ArrayList<>(fields.size());
    for (Map.Entry<String, Float> entry : fields.entrySet()) {
      String field = entry.getKey();
      float boost = entry.getValue();

      Query clause = buildQuery(field, analyzer, queryText);
      clauses.add(new BoostQuery(clause, boost));
    }
    return new DisjunctionMaxQuery(clauses, tiebreaker);
  }
}
