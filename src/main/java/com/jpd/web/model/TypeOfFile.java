package com.jpd.web.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TypeOfFile", description = """
        Category of an uploaded file, used to pick the Firebase Storage folder and the allowed content types.

        * `PDF` - downloadable course material
        * `IMG` - course cover or profile image
        * `CERTIFICATE` - a creator qualification document
        """)
public enum TypeOfFile {
PDF,IMG,CERTIFICATE
}
