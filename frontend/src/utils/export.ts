/**
 * 前端导出工具：把内存中的表格数据导成 CSV。
 *
 * 背景：多个页面原来把「导出」写成了 `ElMessage.success('导出成功')`，
 * 点了没反应也不产生文件，属于假功能。这里统一实现，避免每个页面各写一份。
 *
 * 说明：
 * - 加 UTF-8 BOM，否则 Excel 打开中文会乱码；
 * - 字段里的双引号按 CSV 规范转义成 ""；
 * - 只导出当前页已加载的数据，与后端分页策略保持一致。
 */

export type CsvCell = string | number | boolean | null | undefined

/** 把二维数组导成 CSV 文件并触发下载 */
export function downloadCsv(filename: string, headers: string[], rows: CsvCell[][]): void {
  const escape = (value: CsvCell) => `"${String(value ?? '').replace(/"/g, '""')}"`
  const lines = [headers.map(escape).join(',')]
  for (const row of rows) {
    lines.push(row.map(escape).join(','))
  }
  const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename.endsWith('.csv') ? filename : `${filename}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * 导出表格对象数组。
 *
 * @param filename 文件名（自动补 .csv）
 * @param columns  [{ label: '列名', value: row => 值 }]
 * @param rows     数据行
 */
export function downloadCsvFromRows<T>(
  filename: string,
  columns: { label: string; value: (row: T) => CsvCell }[],
  rows: T[],
): void {
  downloadCsv(
    filename,
    columns.map((c) => c.label),
    rows.map((row) => columns.map((c) => c.value(row))),
  )
}
