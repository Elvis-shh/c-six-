import pymysql, sys, json, asyncio
from pathlib import Path
from app.services.report_fetch_service import report_fetch_service

async def main():
    conn = pymysql.connect(host='mysql', user='smartreport', password='smartreport123',
                           database='smartreport', charset='utf8mb4')
    cur = conn.cursor()

    cur.execute("""
        SELECT fr.id, fr.company_code, fr.report_year, fr.source_file_url
        FROM financial_reports fr
        WHERE fr.source='crawler' AND fr.status=1
        AND NOT EXISTS (SELECT 1 FROM financial_indicators WHERE report_id=fr.id)
        ORDER BY fr.id
    """)
    reports = cur.fetchall()
    print(f'Found {len(reports)} reports without indicators')

    ok = 0
    fail = 0
    for rid, company_code, report_year, file_path in reports:
        if not file_path or not Path(file_path).exists():
            continue
        try:
            data = await report_fetch_service.parse_report_file(file_path)
        except Exception as e:
            print(f'  FAIL [{rid}] {company_code} {report_year}: {e}')
            fail += 1
            continue

        extracted = data.get('extractedData', {})
        if not extracted:
            print(f'  EMPTY [{rid}] {company_code} {report_year}')
            fail += 1
            continue

        from decimal import Decimal, ROUND_HALF_UP
        cur.execute('DELETE FROM financial_indicators WHERE report_id=%s', (rid,))
        saved = 0
        for key, value in extracted.items():
            if value.get('value') is None:
                continue
            confidence = value.get('confidence', 0)
            if confidence is None or confidence < 0.8:
                continue
            v = float(value['value'])
            try:
                dv = Decimal(str(v)).quantize(Decimal('0.0001'), rounding=ROUND_HALF_UP)
            except Exception:
                continue
            rating = 'good' if confidence >= 0.8 else 'warning'
            cur.execute(
                'INSERT INTO financial_indicators (report_id, indicator_key, value, rating) VALUES (%s,%s,%s,%s)',
                (rid, key, dv, rating)
            )
            saved += 1

        # Compute derived indicators
        cur.execute('SELECT indicator_key, value FROM financial_indicators WHERE report_id=%s', (rid,))
        vals = {row[0]: row[1] for row in cur.fetchall()}
        tl = vals.get('totalLiabilities')
        ta = vals.get('totalAssets')
        if tl and ta and ta != 0:
            ratio = (tl / ta * 100).quantize(Decimal('0.0001'), rounding=ROUND_HALF_UP)
            cur.execute('INSERT INTO financial_indicators (report_id, indicator_key, value, rating) VALUES (%s,%s,%s,%s)',
                        (rid, 'debtRatio', ratio, 'derived'))
            saved += 1
        pf = vals.get('profit')
        rv = vals.get('revenue')
        if pf and rv and rv != 0:
            margin = (pf / rv * 100).quantize(Decimal('0.0001'), rounding=ROUND_HALF_UP)
            cur.execute('INSERT INTO financial_indicators (report_id, indicator_key, value, rating) VALUES (%s,%s,%s,%s)',
                        (rid, 'netMargin', margin, 'derived'))
            saved += 1

        conn.commit()
        ok += 1
        print(f'  OK [{rid}] {company_code} {report_year}: {saved} indicators')

        await asyncio.sleep(0.2)

    cur.close()
    conn.close()
    print(f'\nDONE: {ok} ok, {fail} fail')

asyncio.run(main())
