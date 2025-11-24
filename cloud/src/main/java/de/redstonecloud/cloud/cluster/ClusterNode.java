package de.redstonecloud.cloud.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class ClusterNode {
    private String name;
    private String id;
    private String token;
}
