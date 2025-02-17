package com.vercarus.sb322jdk21.backend.dbutil.rdbms.testtable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Data
@Entity
@Table(
        name = "testtable_extend"
)
public class TestTableExtend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String field1;

    @ManyToOne
    @JoinColumn(name = "testtable_id")
    private TestTable testtable;
}
