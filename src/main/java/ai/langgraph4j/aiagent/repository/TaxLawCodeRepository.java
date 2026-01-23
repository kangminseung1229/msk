package ai.langgraph4j.aiagent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.langgraph4j.aiagent.entity.law.TaxLawCode;

public interface TaxLawCodeRepository extends JpaRepository<TaxLawCode, Long> {
    
}
