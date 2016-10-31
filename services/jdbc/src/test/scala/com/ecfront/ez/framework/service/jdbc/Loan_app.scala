package com.ecfront.ez.framework.service.jdbc

import java.util.Date

import scala.beans.BeanProperty

@Entity("贷款申请")
class Loan_app extends SecureModel with StatusModel {
  @UUID
  @Desc("申请编号: 英文简称_INT（11位）", 32, 0)
  @BeanProperty var loan_app_uuid: String = _
  @Desc("申请编号: 英文简称_INT（11位）", 32, 0)
  @Unique
  @BeanProperty var app_number: String = _
  @Desc("贷款商品uuid：车型", 32, 0)
  @BeanProperty var loan_goods_uuid: String = _
  @Desc("贷款产品uuid", 32, 0)
  @BeanProperty var loan_product_uuid: String = _
  @Desc("借款人uuid", 32, 0)
  @BeanProperty var customer_uuid: String = _
  @Desc("销售专员uuid", 32, 0)
  @BeanProperty var sales_uuid: String = _
  @Desc("金融专员uuid", 32, 0)
  @BeanProperty var fin_special_uuid: String = _
  @Desc("4s店/经销商uuid", 32, 0)
  @BeanProperty var supplier_uuid: String = _
  @Desc("开票价", 14, 2)
  @BeanProperty var goods_price_inv: BigDecimal = _
  @Desc("保险金额", 14, 2)
  @BeanProperty var goods_ins_amount: BigDecimal = _
  @Desc("购置税", 14, 2)
  @BeanProperty var goods_tax_amount: BigDecimal = _
  @Desc("首付金额（当贷款产品为反租时填写）", 14, 2)
  @BeanProperty var down_payment_amount: BigDecimal = _
  @Desc("尾款金额（当贷款产品为反租时填写）", 14, 2)
  @BeanProperty var final_payment_amount: BigDecimal = _
  @Desc("保证金额（当贷款产品为正租时填写）", 14, 2)
  @BeanProperty var deposit_payment_amount: BigDecimal = _
  @Desc("标的总额（项目金额=开票价+购置税（融资情况下）+保险金额（融资情况下））", 14, 2)
  @BeanProperty var project_amount: BigDecimal = _
  @Desc("申请金额（标的总额-首付金额）", 14, 2)
  @BeanProperty var loan_amount: BigDecimal = _
  @Desc("还款期数", 11, 0)
  @BeanProperty var repayment_periods: Int = _
  @Desc("每期还款（月供）", 14, 2)
  @BeanProperty var repayment_per_periods: BigDecimal = _
  @Desc("还款总额", 14, 2)
  @BeanProperty var repayment_total_amount: BigDecimal = _
  @Desc("还利总额", 14, 2)
  @BeanProperty var repayment_total_interest: BigDecimal = _
  @Desc("申请日期", 0, 0)
  @BeanProperty var apply_date: Date = _
  @Desc("资金状态", 48, 0)
  @BeanProperty var finance_status: String = _
}

object Loan_app extends SecureStorage[Loan_app] with StatusStorage[Loan_app]












