package com.programm.plugz.test;

import com.programm.plugz.persist.Entity;
import com.programm.plugz.persist.ForeignKey;
import com.programm.plugz.persist.Generated;
import com.programm.plugz.persist.ID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Tag {

    @ID
    @Generated
    private int id;

    private String title;
    private String description;

    @Override
    public String toString() {
        return "Tag{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
