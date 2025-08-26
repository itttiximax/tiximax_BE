package com.tiximax.txm.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter

public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "destinationId")
    private Long destinationId;

    @Column(nullable = false)
    private String destinationName;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Orders> orders;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL)
    @JsonIgnore
    Set<Packing> packings;

}
