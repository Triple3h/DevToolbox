package com.tripleh.devtoolbox.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DocCommentPathTest {

    @Test
    fun extractsGetPathFromJavadoc() {
        val comment = """
            /**
             * 根据顶级地区查询子地区列表
             * 接口说明：获取地区列表数据
             * GET /appr-union-service/union/area/areaList/loadAreaList.v
             */
        """.trimIndent()
        assertEquals("/appr-union-service/union/area/areaList/loadAreaList.v", findDocCommentPath(comment))
    }

    @Test
    fun extractsPostPathWithoutSuffix() {
        val comment = """
            /**
             * 同步材料绑定版式文档接口
             * POST /appr-union-service/api/union/stuff/initFormAttaToStuff
             */
        """.trimIndent()
        assertEquals("/appr-union-service/api/union/stuff/initFormAttaToStuff", findDocCommentPath(comment))
    }

    @Test
    fun returnsEmptyWhenNoPathLine() {
        val comment = """
            /**
             * 只是普通注释，没有接口路径
             * GET 与 POST 的区别说明
             */
        """.trimIndent()
        assertEquals("", findDocCommentPath(comment))
    }

    @Test
    fun returnsEmptyForBlankInput() {
        assertEquals("", findDocCommentPath(""))
    }
}
