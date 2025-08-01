package com.treasurehunt.repository;

import com.treasurehunt.entity.TeamMember;
import com.treasurehunt.entity.UserRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    
    List<TeamMember> findByRegistrationOrderByMemberPosition(UserRegistration registration);
    
    @Query("SELECT tm FROM TeamMember tm WHERE tm.registration.id = :registrationId ORDER BY tm.memberPosition")
    List<TeamMember> findByRegistrationIdOrderByMemberPosition(@Param("registrationId") Long registrationId);
    
    @Query("SELECT tm FROM TeamMember tm WHERE tm.registration.id = :registrationId AND tm.memberPosition = 1")
    TeamMember findTeamLeaderByRegistrationId(@Param("registrationId") Long registrationId);
    
    void deleteByRegistration(UserRegistration registration);
}
