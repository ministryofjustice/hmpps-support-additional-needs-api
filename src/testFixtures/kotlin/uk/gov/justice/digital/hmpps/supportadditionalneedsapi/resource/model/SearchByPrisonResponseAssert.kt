package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractAssert

fun assertThat(actual: SearchByPrisonResponse?) = SearchByPrisonResponseAssert(actual)

class SearchByPrisonResponseAssert(actual: SearchByPrisonResponse?) : AbstractAssert<SearchByPrisonResponseAssert, SearchByPrisonResponse?>(actual, SearchByPrisonResponseAssert::class.java) {
  fun isFirstPage(): SearchByPrisonResponseAssert {
    isNotNull
    with(actual!!) {
      if (!pagination.first) {
        failWithMessage("Expected pagination page to be the first page, but was not")
      }
    }
    return this
  }

  fun isNotFirstPage(): SearchByPrisonResponseAssert {
    isNotNull
    with(actual!!) {
      if (pagination.first) {
        failWithMessage("Expected pagination page not to be the first page, but it was")
      }
    }
    return this
  }

  fun isLastPage(): SearchByPrisonResponseAssert {
    isNotNull
    with(actual!!) {
      if (!pagination.last) {
        failWithMessage("Expected pagination page to be the last page, but was not")
      }
    }
    return this
  }

  fun isNotLastPage(): SearchByPrisonResponseAssert {
    isNotNull
    with(actual!!) {
      if (pagination.last) {
        failWithMessage("Expected pagination page not to be the last page, but it was")
      }
    }
    return this
  }

  fun isPage(expected: Int): SearchByPrisonResponseAssert {
    isNotNull
    with(actual!!) {
      if (pagination.page != expected) {
        failWithMessage("Expected pagination page to be $expected, but was ${pagination.page}")
      }
    }
    return this
  }

  fun hasPageSize(expected: Int): SearchByPrisonResponseAssert {
    isNotNull
    with(actual!!) {
      if (pagination.pageSize != expected) {
        failWithMessage("Expected pagination pageSize to be $expected, but was ${pagination.pageSize}")
      }
    }
    return this
  }

  fun hasTotalElements(expected: Int): SearchByPrisonResponseAssert {
    isNotNull
    with(actual!!) {
      if (pagination.totalElements != expected) {
        failWithMessage("Expected pagination totalElements to be $expected, but was ${pagination.totalElements}")
      }
    }
    return this
  }

  fun hasTotalPages(expected: Int): SearchByPrisonResponseAssert {
    isNotNull
    with(actual!!) {
      if (pagination.totalPages != expected) {
        failWithMessage("Expected pagination totalPages to be $expected, but was ${pagination.totalPages}")
      }
    }
    return this
  }

  fun currentPageHasNumberOfRecords(expected: Int): SearchByPrisonResponseAssert {
    isNotNull
    with(actual!!) {
      if (people.size != expected) {
        failWithMessage("Expected current page to have $expected records, but was ${people.size}")
      }
    }
    return this
  }
}
