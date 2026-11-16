package com.commandos.organization.domain;

import com.commandos.identity.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_memberships")
public class MembershipEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_user_id")
    private UserEntity manager;

    @Column(name = "employment_type", nullable = false, length = 24)
    private String employmentType;

    @Column(name = "start_date")
    private java.time.LocalDate startDate;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "joined_at")
    private Instant joinedAt;

    protected MembershipEntity() {
    }

    public MembershipEntity(
            UUID id, OrganizationEntity organization, UserEntity user, RoleEntity role, Instant joinedAt) {
        this.id = id;
        this.organization = organization;
        this.user = user;
        this.role = role;
        this.status = "ACTIVE";
        this.employmentType = "FULL_TIME";
        this.joinedAt = joinedAt;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public UserEntity getUser() {
        return user;
    }

    public RoleEntity getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public DepartmentEntity getDepartment() {
        return department;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public UserEntity getManager() {
        return manager;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public java.time.LocalDate getStartDate() {
        return startDate;
    }

    public void updateEmployeeProfile(
            DepartmentEntity department,
            String jobTitle,
            UserEntity manager,
            String employmentType,
            java.time.LocalDate startDate) {
        this.department = department;
        this.jobTitle = jobTitle;
        this.manager = manager;
        this.employmentType = employmentType;
        this.startDate = startDate;
    }
}
