package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

// Role Constants
const val SEARCH_RO = "ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO"

const val ELSP_RO = "ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"
const val ELSP_RW = "ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"

// Authority Checks
const val HAS_SEARCH_PRISONS = """hasAuthority('$SEARCH_RO')"""

const val HAS_VIEW_ELSP = """hasAnyAuthority("$ELSP_RO", "$ELSP_RW")"""

const val HAS_EDIT_ELSP = """hasAuthority('$ELSP_RW')"""
