package com.jpd.web.testutil;

import com.jpd.web.model.Creator;
import com.jpd.web.model.KahootListFunction;

import java.util.ArrayList;

public final class KahootTestDataBuilder {

    private KahootTestDataBuilder() {
    }

    public static KahootListFunction.KahootListFunctionBuilder aKahoot() {
        return KahootListFunction.builder()
                .kahootId(1L)
                .title("Test Kahoot")
                .creator(CreatorTestDataBuilder.aCreator().build())
                .moduleContent(new ArrayList<>());
    }

    public static KahootListFunction aKahootOwnedBy(String creatorId) {
        return aKahoot().creator(CreatorTestDataBuilder.aCreatorWithId(creatorId)).build();
    }
}
