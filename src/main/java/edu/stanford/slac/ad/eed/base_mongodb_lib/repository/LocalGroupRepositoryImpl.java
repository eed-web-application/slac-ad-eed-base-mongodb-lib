package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroupQueryParameter;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
@AllArgsConstructor
public class LocalGroupRepositoryImpl implements LocalGroupRepositoryCustom {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<LocalGroup> findAll(LocalGroupQueryParameter queryParameter) {
        if (
                queryParameter.getContextSize() != null &&
                        queryParameter.getContextSize() >0 &&
                        queryParameter.getAnchorID() == null
        ) {
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage("The context count cannot be used without the anchor")
                    .errorDomain("WorkRepositoryImpl::searchAll")
                    .build();
        }

        // all the criteria
        List<Criteria> allCriteria = new ArrayList<>();
        LocalDateTime anchorCreatedDate = queryParameter.getAnchorID() != null?getAnchorCreatedDate(queryParameter.getAnchorID()):null;
        List<LocalGroup> elementsBeforeAnchor = contextSearch(queryParameter, anchorCreatedDate, allCriteria);
        List<LocalGroup> elementsAfterAnchor =  limitSearch(queryParameter, anchorCreatedDate, allCriteria);
        elementsBeforeAnchor.addAll(elementsAfterAnchor);
        return elementsBeforeAnchor;
    }

    /**
     * Get the query to search the work
     * @param anchorId the query parameter
     * @return the query
     */
    private LocalDateTime getAnchorCreatedDate(String anchorId) {
        Query q = new Query();
        q.addCriteria(Criteria.where("id").is(anchorId));
        q.fields().include("createdDate");
        var inventoryElementFound =  mongoTemplate.findOne(q, LocalGroup.class);
        return (inventoryElementFound!=null)?inventoryElementFound.getCreatedDate():null;
    }

    /**
     * Get the default query
     * @param queryParameter is the query parameter class
     * @return return the mongodb query
     */
    private static Query getQuery(LocalGroupQueryParameter queryParameter) {
        Query query;
        if (queryParameter.getSearch() != null && !queryParameter.getSearch().isEmpty()) {
            query = TextQuery.queryText(TextCriteria.forDefaultLanguage()
                    .matchingAny(queryParameter.getSearch().split(" "))
            );
        } else {
            query = new Query();
        }
        return query;
    }

    /**
     * Limit the search
     * @param queryParameter the query parameter
     * @param anchorCreatedDate the anchor created date
     * @param allCriteria the criteria
     * @return the list of work
     */
    private List<LocalGroup> limitSearch(LocalGroupQueryParameter queryParameter, LocalDateTime anchorCreatedDate, List<Criteria> allCriteria) {
        List<LocalGroup> elementsAfterAnchor = new ArrayList<>();
        if (queryParameter.getLimit() != null && queryParameter.getLimit() > 0) {
            Query query = getQuery(queryParameter);
            if (anchorCreatedDate != null) {
                allCriteria.add(
                        Criteria.where("createdDate").gt(anchorCreatedDate)
                );
            }
            if(!allCriteria.isEmpty()) {
                query.addCriteria(
                        new Criteria().andOperator(
                                allCriteria
                        )
                );
            }

            query.with(
                    Sort.by(
                            Sort.Direction.ASC, "createdDate")
            ).limit(queryParameter.getLimit());
            elementsAfterAnchor.addAll(
                    mongoTemplate.find(
                            query,
                            LocalGroup.class
                    )
            );
        }
        return elementsAfterAnchor;
    }

    /**
     * Search the context
     * @param queryParameter the query parameter
     * @param anchorCreatedDate the anchor created date
     * @param allCriteria the criteria
     * @return the list of work
     */
    private List<LocalGroup> contextSearch(LocalGroupQueryParameter queryParameter, LocalDateTime anchorCreatedDate, List<Criteria> allCriteria) {
        List<LocalGroup> elementsBeforeAnchor = new ArrayList<>();
        if (
                queryParameter.getContextSize() != null
                        && queryParameter.getContextSize() > 0
                        && anchorCreatedDate != null
        ) {
            allCriteria.add(
                    Criteria.where("createdDate").lte(anchorCreatedDate)
            );

            // at this point the anchor id is not null
            Query query = getQuery(queryParameter);
            if(!allCriteria.isEmpty()) {
                query.addCriteria(
                        new Criteria().andOperator(
                                allCriteria
                        )
                );
            }
            query.with(
                    Sort.by(
                            Sort.Direction.DESC, "createdDate")
            ).limit(queryParameter.getContextSize());
            elementsBeforeAnchor.addAll(
                    mongoTemplate.find(
                            query,
                            LocalGroup.class
                    )
            );
            // reverse the order
            Collections.reverse(elementsBeforeAnchor);
        }
        return elementsBeforeAnchor;
    }
}
