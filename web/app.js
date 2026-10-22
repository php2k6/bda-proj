const $ = id => document.getElementById(id);
const fmt = n => Number(n).toLocaleString('en-IN');
let mode = 'existing', selected = [], searchTimer;
function element(tag, text, cls) { const e = document.createElement(tag); if (text !== undefined) e.textContent = text; if (cls) e.className = cls; return e; }
async function api(url, options) { const response = await fetch(url, options); const data = await response.json(); if (!response.ok) throw new Error(data.error || 'Request failed'); return data; }
function view(analytics) { $('recommend-view').hidden = analytics; $('analytics-view').hidden = !analytics; $('show-recommend').classList.toggle('active', !analytics); $('show-analytics').classList.toggle('active', analytics); }
$('show-recommend').onclick = () => view(false); $('show-analytics').onclick = () => view(true);
function setMode(next) { mode = next; $('existing-controls').hidden = next !== 'existing'; $('profile-controls').hidden = next !== 'profile'; $('existing-mode').classList.toggle('active', next === 'existing'); $('profile-mode').classList.toggle('active', next === 'profile'); $('error').textContent = ''; }
$('existing-mode').onclick = () => setMode('existing'); $('profile-mode').onclick = () => setMode('profile');
function showMovies(data) {
  const target = $('movies'); target.replaceChildren(); $('result-count').textContent = `${data.recommendations.length} films`;
  $('method').textContent = data.method + (data.userId !== undefined ? ` · User ${data.userId} · ${data.ratedCount} previous ratings` : ` · ${data.ratedCount} films you rated`);
  const table = element('table'), head = element('thead'), heading = element('tr');
  ['Movie', 'Genre', 'Score', 'Ratings', 'Average'].forEach(t => heading.append(element('th',t)));
  head.append(heading); table.append(head); const body = element('tbody');
  data.recommendations.forEach(m => {
    const row = element('tr');
    [m.title, m.genres.split('|').join(', '), m.score.toFixed(2), fmt(m.ratingCount), m.averageRating.toFixed(1)].forEach(v => row.append(element('td',v)));
    body.append(row);
  });
  table.append(body); target.append(table);
  if (!data.recommendations.length) target.append(element('p', 'No movies match these filters. Try a different genre or rating count.', 'empty'));
}
async function recommend() {
  $('error').textContent = ''; $('recommend').disabled = true;
  try {
    const fields = {limit: Number($('limit').value), genre: $('genre').value, minimum: Number($('minimum').value)};
    if (mode === 'existing') showMovies(await api('/api/recommend?' + new URLSearchParams({...fields, user: $('user').value})));
    else {
      if (!selected.length) throw new Error('Add at least one movie and give it a rating.');
      showMovies(await api('/api/profile', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({...fields, ratings:selected.map(m => ({movieId:m.movieId, rating:m.rating}))})}));
    }
  } catch (e) { $('error').textContent = e.message; } finally { $('recommend').disabled = false; }
}
$('recommend').onclick = recommend;
function renderRatings() {
  $('selected-ratings').replaceChildren();
  selected.forEach(m => {
    const row = element('div', undefined, 'rated-row'), select = element('select'); select.setAttribute('aria-label', `Rating for ${m.title}`);
    for (let r = .5; r <= 5; r += .5) { const option = element('option', r.toFixed(1)); option.value = r; option.selected = r === m.rating; select.append(option); }
    select.onchange = () => m.rating = Number(select.value);
    const remove = element('button', '×'); remove.setAttribute('aria-label', `Remove ${m.title}`); remove.onclick = () => { selected = selected.filter(x => x.movieId !== m.movieId); renderRatings(); };
    row.append(element('span', m.title), select, remove); $('selected-ratings').append(row);
  });
}
$('search').oninput = () => { clearTimeout(searchTimer); searchTimer = setTimeout(async () => {
  $('search-results').replaceChildren(); if ($('search').value.trim().length < 2) return;
  try { const movies = await api('/api/movies?q=' + encodeURIComponent($('search').value));
    movies.filter(m => !selected.some(s => s.movieId === m.movieId)).slice(0, 6).forEach(m => {
      const button = element('button', m.title); button.onclick = () => { if (selected.length >= 30) { $('error').textContent='Maximum 30 films.'; return; } selected.push({...m, rating:4}); renderRatings(); $('search').value=''; $('search-results').replaceChildren(); }; $('search-results').append(button);
    });
  } catch(e) { $('error').textContent=e.message; }
}, 220); };
$('example').onclick = () => { selected = [{movieId:1,title:'Toy Story (1995)',rating:4.5},{movieId:260,title:'Star Wars: Episode IV - A New Hope (1977)',rating:5},{movieId:296,title:'Pulp Fiction (1994)',rating:4}]; renderRatings(); };
function bars(id, values, key, label) { const max = Math.max(...values.map(x => x[key])); const target = $(id); target.replaceChildren(); values.forEach(v => { const row = element('div',undefined,'bar-row'), track=element('div',undefined,'bar-track'),fill=element('div',undefined,'bar-fill'); fill.style.width=(v[key]/max*100)+'%'; track.append(fill); row.append(element('span',String(v[label])),track,element('span',fmt(v[key]),'bar-count')); target.append(row); }); }
function details(id, rows) { $(id).replaceChildren(); rows.forEach(([name,value]) => { const row=element('div'); row.append(element('dt',name),element('dd',String(value))); $(id).append(row); }); }
async function init() {
  try {
    const [summary,users] = await Promise.all([api('/api/summary'),api('/api/users')]), a=summary.analytics,m=summary.metrics;
    $('stat-ratings').textContent=fmt(a.ratings); $('stat-movies').textContent=fmt(a.movies); $('stat-users').textContent=fmt(a.users);
    users.forEach(u=>{const option=element('option',`User ${u.userId} · ${u.ratingCount} ratings`);option.value=u.userId;$('user').append(option);});
    summary.genres.forEach(g=>{const option=element('option',g);option.value=g;$('genre').append(option);});
    bars('rating-bars',a.ratingDistribution,'count','rating');bars('genre-bars',a.genres.slice(0,10),'count','genre');
    details('evaluation',[['RMSE (ALS)',m.rmse.toFixed(4)],['MAE (ALS)',m.mae.toFixed(4)],['Global-mean baseline RMSE',m.globalMeanBaselineRmse.toFixed(4)],['Test prediction coverage',(100*m.predictionCoverage).toFixed(2)+'%'],['Training / test ratings',fmt(m.trainRatings)+' / '+fmt(m.testRatings)],['Cold-start rows excluded',fmt(m.coldStartRowsDropped)]]);
    details('preparation',[['Invalid rows removed',a.invalidRatingsRemoved],['Duplicate pairs removed',a.duplicateRatingsRemoved],['Unknown movie rows removed',a.unknownMovieRatingsRemoved],['Average rating',a.averageRating.toFixed(3)],['Latent factors / iterations',m.rank+' / '+m.iterations],['Regularisation',m.regParam]]);
    a.mostRated.forEach(m=>{const tr=element('tr');[m.title,m.genres.split('|').join(', '),fmt(m.ratingCount),m.averageRating.toFixed(2)].forEach(v=>tr.append(element('td',v)));$('popular-table').append(tr);});
    await recommend();
  } catch(e) { $('error').textContent=e.message; $('method').textContent='The dataset could not be loaded.'; }
}
init();
